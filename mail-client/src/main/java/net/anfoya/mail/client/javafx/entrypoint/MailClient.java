package net.anfoya.mail.client.javafx.entrypoint;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.media.AudioClip;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Notification.Notifier;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.settings.VersionHelper;
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

	private static final int DEFAULT_WIDTH = 1400;
	private static final int DEFAULT_HEIGHT = 800;

	private GmailService gmail;
	private Stage stage;

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void init() throws Exception {
		Settings.getSettings().load();
		initGmail();
	}

	@Override
	public void start(final Stage stage) throws Exception {
		this.stage = stage;

		stage.setOnCloseRequest(e -> confirmClose(e));

		stage.initStyle(StageStyle.UNIFIED);
		initMacOs();

		showBrowser();

		initNewThreadNotifier();
		checkVersion();
	}

	private void confirmClose(final WindowEvent e) {
		if (Settings.getSettings().confirmOnQuit().get()) {
			final CheckBox checkBox = new CheckBox("don't show again");
			checkBox.selectedProperty().addListener((ov, o, n) -> {
				Settings.getSettings().confirmOnQuit().set(!n);
				Settings.getSettings().save();
			});

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("FisherMail");
			alert.setHeaderText("closing this window will stop FisherMail\ryou will no longer receive new mail notification");
			alert.getDialogPane().contentProperty().set(checkBox);
			alert.showAndWait()
				.filter(response -> response == ButtonType.CANCEL)
				.ifPresent(response -> e.consume());
		}
	}

	private void signout() {
		boolean signout = false;
		if (Settings.getSettings().confirmOnSignout().get()) {
			final CheckBox checkBox = new CheckBox("don't show again");
			checkBox.selectedProperty().addListener((ov, o, n) -> {
				Settings.getSettings().confirmOnSignout().set(!n);
				Settings.getSettings().save();
			});

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("FisherMail");
			alert.setHeaderText("sign out from your e-mail account\rthis will close the mail browser");
			alert.getDialogPane().contentProperty().set(checkBox);
			final Optional<ButtonType> response = alert.showAndWait();
			signout = response.isPresent() && response.get() == ButtonType.OK;
		} else {
			signout = true;
		}
		if (signout) {
			stage.hide();
			gmail.disconnect();
			initGmail();
			showBrowser();
		}
	}

	private void initGmail() {
		if (gmail == null) {
			gmail = new GmailService();
		}
		try {
			gmail.connect(App.MAIL_CLIENT);
		} catch (final MailException e) {
			LOGGER.error("login failed", e);
		}
	}

	private void checkVersion() {
		final VersionHelper checker = new VersionHelper();
		checker.isLastestProperty().addListener((ov, o, n) -> {
			if (!n) {
				Platform.runLater(() -> {
					Notifier.INSTANCE.notifySuccess(
							"FisherMail - " + checker.getLatestVersion()
							, "click here to download"
							, v -> {
								UrlHelper.open(Settings.DOWNLOAD_URL);
								return null;
							});
				});
			}
		});
	}

	private void showBrowser() {
		if (gmail.disconnectedProperty().get()) {
			return;
		}

		MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> mailBrowser;
		try {
			mailBrowser = new MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact>(gmail);
		} catch (final MailException e) {
			LOGGER.error("load mail browser", e);
			return;
		}
		mailBrowser.setOnSignout(e -> signout());

		stage.titleProperty().unbind();
		stage.setScene(mailBrowser);
		initSize(stage);
		stage.show();
		mailBrowser.initData();

		initTitle(stage);
	}

	private void initTitle(final Stage stage) {
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
		titleTask.setOnFailed(e -> LOGGER.error("load user's name", e.getSource().getException()));
		ThreadPool.getInstance().submitLow(titleTask, "load user's name");
	}

	private void initSize(final Stage stage) {
		final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

		LOGGER.info("init size to {}x{}", DEFAULT_WIDTH, DEFAULT_HEIGHT);
		stage.setWidth(DEFAULT_WIDTH);
		stage.setHeight(DEFAULT_HEIGHT);

		if (stage.getWidth() > bounds.getWidth()
				|| stage.getHeight() > bounds.getHeight()) {
			LOGGER.info("maximize to {}x{}", bounds.getWidth(), bounds.getHeight());
			stage.setWidth(bounds.getWidth());
			stage.setHeight(bounds.getHeight());
//			stage.setMaximized(true);
		}

		if (stage.getX() < bounds.getMinX() || stage.getX() > bounds.getMaxX()) {
			LOGGER.info("move to minX {}", bounds.getMinX());
			stage.setX(bounds.getMinX());
		}
		if (stage.getY() < bounds.getMinY() || stage.getY() > bounds.getMaxY()) {
			LOGGER.info("move to minY {}", bounds.getMinY());
			stage.setY(bounds.getMinY());
		}
	}

	private void initMacOs() {
		if (!System.getProperty("os.name").contains("OS X")) {
			return;
		}
//		final MenuItem aboutItem = new MenuItem("About FisherMail");
//		final MenuItem preferencesItem = new MenuItem("Preferences...");
//		final MenuItem browserItem = new MenuItem("Mail Browser");
//		browserItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
//		final MenuItem composerItem = new MenuItem("Mail Composer");
//		composerItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
//		final MenuItem tipsItem = new MenuItem("Tips and Tricks");
//		final MenuBar menuBar = new MenuBar(
//				new Menu("Window", (Node)null
//						, aboutItem
//						, new SeparatorMenuItem()
//						, preferencesItem
//						, new SeparatorMenuItem()
//						, composerItem
//						, browserItem)
//				, new Menu("Help", (Node)null
//						, tipsItem));
//		menuBar.setUseSystemMenuBar(true);
//		stage.sceneProperty().addListener(c -> {
//			stage.getScene().setRoot(new BorderPane(stage.getScene().getRoot(), menuBar, null, null, null));
//		});
//		LOGGER.info("initialize OS X stage behaviour");
//		Platform.setImplicitExit(false);
//		com.apple.eawt.Application.getApplication().addAppEventListener(new AppReOpenedListener() {
//			@Override
//			public void appReOpened(final AppReOpenedEvent e) {
//				LOGGER.info("OS X AppReOpenedListener");
//				if (!stage.isShowing()) {
//					LOGGER.debug("OS X show()");
//					Platform.runLater(() -> stage.show());
//				}
//				if (stage.isIconified()) {
//					LOGGER.debug("OS X setIconified(false)");
//					Platform.runLater(() -> stage.setIconified(false));
//				}
//				if (!stage.isFocused()) {
//					LOGGER.debug("OS X requestFocus()");
//					Platform.runLater(() -> stage.requestFocus());
//				}
//			}
//		});
//
//		final List<MenuBase> menus = new ArrayList<>();
//		menus.add(GlobalMenuAdapter.adapt(new Menu("java")));
//
//		final TKSystemMenu menu = Toolkit.getToolkit().getSystemMenu();
//		menu.setMenus(menus);
	}

	private void initNewThreadNotifier() {
		if (gmail.disconnectedProperty().get()) {
			return;
		}
		gmail.addOnNewMessage(threads -> {
			LOGGER.info("notify new thread");
			new AudioClip(Settings.MP3_NEW_MAIL).play();
			threads.forEach(t -> {
				final Task<String> task = new Task<String>() {
					@Override
					protected String call() throws Exception {
						final Message m = gmail.getMessage(t.getLastMessageId());
						return String.format("from %s\r\n%s"
								, String.join(", ", MessageHelper.getNames(m.getMimeMessage().getFrom()))
								, m.getSnippet());
					}
				};
				task.setOnSucceeded(e -> Notifier.INSTANCE.notifySuccess(
						t.getSubject()
						, task.getValue()
						, v -> {
							if (stage.isIconified()) {
								stage.setIconified(false);
							}
							if (!stage.isFocused()) {
								stage.requestFocus();
							}
							return null;
						}));
				task.setOnFailed(e -> LOGGER.error("notify new thread {}", t.getId(), e.getSource().getException()));
				ThreadPool.getInstance().submitLow(task, "notify new thread");
			});

			return null;
		});
	}
}
