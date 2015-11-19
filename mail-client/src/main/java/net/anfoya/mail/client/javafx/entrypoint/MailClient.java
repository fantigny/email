package net.anfoya.mail.client.javafx.entrypoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.eawt.AppEvent.AppReOpenedEvent;
import com.apple.eawt.AppReOpenedListener;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Notification.Notifier;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.settings.VersionChecker;
import net.anfoya.mail.browser.javafx.util.UrlHelper;
import net.anfoya.mail.client.App;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailContact;
import net.anfoya.mail.gmail.model.GmailMessage;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.Message;

public class MailClient extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailClient.class);

	private GmailService gmail;

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void init() throws Exception {
		Settings.getSettings().load();
		gmail = new GmailService();
	}

	@Override
	public void start(final Stage stage) throws Exception {
		stage.initStyle(StageStyle.UNIFIED);
		initMacOs(stage);

		showBrowser(stage);
		initNotifier(stage);
		checkVersion();
	}

	private void checkVersion() {
		final VersionChecker checker = new VersionChecker();
		final Task<Boolean> isLatestTask = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				return checker.isLastVersion();
			}
		};
		isLatestTask.setOnSucceeded(e -> {
			if (!(boolean)e.getSource().getValue()) {
				Notifier.INSTANCE.notifyInfo(
						"FisherMail " + checker.getLastestVesion()
						, "available at " + Settings.URL
						, v -> {
							UrlHelper.open("http://" + Settings.URL);
							return null;
						});
			}
		});
		isLatestTask.setOnFailed(e -> LOGGER.error("getting latest version info", e));
		ThreadPool.getInstance().submitLow(isLatestTask, "checking version");
	}

	private void showBrowser(Stage stage) {
		try {
			gmail.connect(App.MAIL_CLIENT);
		} catch (final MailException e) {
			LOGGER.error("login failed", e);
			return;
		}
		if (gmail.disconnected().get()) {
			return;
		}

		MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> mailBrowser;
		try {
			mailBrowser = new MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact>(gmail);
		} catch (final MailException e) {
			LOGGER.error("loading mail browser", e);
			return;
		}
		mailBrowser.setOnSignout(e -> {
			gmail.disconnect();
			stage.hide();
			showBrowser(stage);
			Notifier.INSTANCE.stop();
		});
		mailBrowser.setOnSignouAndClose(e -> {
			gmail.disconnect();
			stage.hide();
			Notifier.INSTANCE.stop();
		});

		initTitle(stage);

		stage.titleProperty().unbind();
		stage.setWidth(1400);
		stage.setHeight(800);
		stage.setScene(mailBrowser);
		stage.show();

		mailBrowser.initData();
	}

	private void initTitle(Stage stage) {
		final Task<String> titleTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				final Contact contact = gmail.getContact();
				if (contact.getFullname().isEmpty()) {
					return contact.getEmail();
				} else {
					return contact.getFullname() + " (" + contact.getEmail() + ")";
				}
			}
		};
		titleTask.setOnSucceeded(e -> stage.setTitle("FisherMail - " + e.getSource().getValue()));
		titleTask.setOnFailed(e -> LOGGER.error("loading user's name", e.getSource().getException()));
		ThreadPool.getInstance().submitLow(titleTask, "loading user's name");
	}

	private void initMacOs(Stage stage) {
		if (!System.getProperty("os.name").contains("OS X")) {
			return;
		}
		LOGGER.info("initialize OS X stage behaviour");
		Platform.setImplicitExit(false);
		com.apple.eawt.Application.getApplication().addAppEventListener(new AppReOpenedListener() {
			@Override
			public void appReOpened(final AppReOpenedEvent e) {
				LOGGER.info("OS X AppReOpenedListener");
				if (!stage.isShowing()) {
					LOGGER.debug("OS X show()");
					Platform.runLater(() -> stage.show());
				}
				if (stage.isIconified()) {
					LOGGER.debug("OS X setIconified(false)");
					Platform.runLater(() -> stage.setIconified(false));
				}
				if (!stage.isFocused()) {
					LOGGER.debug("OS X requestFocus()");
					Platform.runLater(() -> stage.requestFocus());
				}
			}
		});
//
//		final List<MenuBase> menus = new ArrayList<>();
//		menus.add(GlobalMenuAdapter.adapt(new Menu("java")));
//
//		final TKSystemMenu menu = Toolkit.getToolkit().getSystemMenu();
//		menu.setMenus(menus);
	}

	private void initNotifier(Stage primaryStage) {
		gmail.addOnNewMessage(threads -> {
			LOGGER.debug("notifyAfterNewMessage");

			threads.forEach(t -> {
				ThreadPool.getInstance().submitLow(() -> {
					final String message;
					try {
						final Message m = gmail.getMessage(t.getLastMessageId());
						message = "from " + String.join(", ", MessageHelper.getNames(m.getMimeMessage().getFrom()))
								+ "\r\n" + m.getSnippet();
					} catch (final Exception e) {
						LOGGER.error("notifying new message for thread {}", t.getId(), e);
						return;
					}
					Platform.runLater(() -> Notifier.INSTANCE.notifySuccess(
							t.getSubject()
							, message
							, v -> {
								if (primaryStage.isIconified()) {
									primaryStage.setIconified(false);
								}
								if (!primaryStage.isFocused()) {
									primaryStage.requestFocus();
								}
								return null;
							}));
				}, "notifying new message");
			});

			return null;
		});
	}
}
