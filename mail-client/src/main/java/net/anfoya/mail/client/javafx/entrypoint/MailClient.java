package net.anfoya.mail.client.javafx.entrypoint;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
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
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import net.anfoya.java.util.concurrent.ObservableExecutors;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.javafx.notification.NotificationService;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.MailBrowser.Mode;
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

	private Settings settings;
	private NotificationService notificationService;
	private GmailService gmail;

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void init() throws Exception {
		initThreadPool();
		initSettings();
		initProxy();
		initGmail();
	}

	@Override
	public void start(final Stage primaryStage) {
		gmail.setOnAuth(() -> {
			Platform.runLater(() -> {
				primaryStage.setOnCloseRequest(e -> confirmClose(e, primaryStage));
				initNewThreadNotifier(primaryStage);
				showBrowser(primaryStage);
				checkVersion();
			});
		});
		gmail.setOnAuthFailed(() -> {
			LOGGER.error(gmail.getAuthException().getMessage());
			Platform.runLater(() -> primaryStage.close());
		});

		gmail.authenticate();
	}

	private void confirmClose(final WindowEvent e, final Stage stage) {
		if (settings.confirmOnQuit().get()) {
			final CheckBox checkBox = new CheckBox("don't show again");
			checkBox.selectedProperty().addListener((ov, o, n) -> settings.confirmOnQuit().set(!n));

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.initOwner(stage);
			alert.setTitle("FisherMail");
			alert.setHeaderText("you are about to close FisherMail\rnew e-mail notification will be stopped");
			alert.getDialogPane().contentProperty().set(checkBox);
			alert.showAndWait()
			.filter(response -> response == ButtonType.CANCEL)
			.ifPresent(response -> e.consume());
		}
	}

	private void signout(Stage stage) {
		boolean signout = true;
		if (settings.confirmOnSignout().get()) {
			final CheckBox checkBox = new CheckBox("don't show again");
			checkBox.selectedProperty().addListener((ov, o, n) -> settings.confirmOnSignout().set(!n));

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("FisherMail");
			alert.setHeaderText("sign out from your e-mail account\rthis will close the mail browser");
			alert.getDialogPane().contentProperty().set(checkBox);
			final Optional<ButtonType> response = alert.showAndWait();
			signout = response.isPresent() && response.get() == ButtonType.OK;
		}
		if (!signout) {
			return;
		}

		stage.hide();
		gmail.signout();
	}

	private void initThreadPool() {
		ThreadPool.setDefault(
				ObservableExecutors.newCachedThreadPool("min", Thread.MIN_PRIORITY)
				,  null
				,  ObservableExecutors.newCachedThreadPool("max", Thread.MAX_PRIORITY));
	}

	private void initSettings() {
		settings = new Settings(gmail);
		settings.load();
	}

	private void checkVersion() {
		final VersionHelper version = new VersionHelper();
		version.isLastest().addListener((ov, o, n) -> {
			if (!n) {
				notificationService.notifySuccess(
						"FisherMail - " + version.getLatestVersion()
						, "click here to download"
						, () -> UrlHelper.open(Settings.DOWNLOAD_URL));
			}
		});
		version.start();
	}

	private void showBrowser(final Stage stage) {
		MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> mailBrowser;
		try {
			mailBrowser = new MailBrowser<>(gmail, notificationService, settings);
		} catch (final MailException e) {
			LOGGER.error("load mail browser", e);
			return;
		}
		mailBrowser.setOnSignout(() -> signout(stage));
		mailBrowser.addOnModeChange(() -> refreshTitle(stage, mailBrowser));

		final Image icon = new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/Mail.png"));
		stage.getIcons().add(icon);
		stage.initStyle(StageStyle.DECORATED);
		stage.setScene(mailBrowser);
		stage.setOnHiding(e -> ThreadPool.getDefault().mustRun("save global settings", () -> {
			gmail.stopListening();
			mailBrowser.saveSettings();
			settings.windowX().set(stage.getX());
			settings.windowY().set(stage.getY());
			settings.windowWidth().set(stage.getWidth());
			settings.windowHeight().set(stage.getHeight());
			settings.saveNow();
		}));

		initSize(stage);
		stage.show();

		gmail.startListening();
	}

	private void refreshTitle(final Stage stage, MailBrowser<?, ?, ?, ?, ?> browser) {
		final Task<String> titleTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				final Mode mode = browser.modeProperty().get();
				final Contact contact = gmail.getContact();
				if (contact.getFullname().isEmpty() || mode != Mode.FULL) {
					return contact.getEmail();
				} else {
					return "FisherMail - " + contact.getFullname();
				}
			}
		};
		titleTask.setOnSucceeded(e -> stage.setTitle((String)e.getSource().getValue()));
		titleTask.setOnFailed(e -> LOGGER.error("load user's name", e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MIN, "load user's name", titleTask);
	}

	private void initSize(final Stage stage) {
		final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

		LOGGER.info("init size to {}x{}", settings.windowWidth().get(), settings.windowHeight().get());
		stage.setWidth(settings.windowWidth().get());
		stage.setHeight(settings.windowHeight().get());

		if (settings.windowX().isNotEqualTo(-1).get()) {
			stage.setX(settings.windowX().get());
			stage.setY(settings.windowY().get());
		}

		if (stage.getWidth() > bounds.getWidth()
				|| stage.getHeight() > bounds.getHeight()) {
			LOGGER.info("maximize to {}x{}", bounds.getWidth(), bounds.getHeight());
			stage.setWidth(bounds.getWidth());
			stage.setHeight(bounds.getHeight());
			// stage.setMaximized(true);  // buggy on osx 10.7
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

	private void initNewThreadNotifier(Stage stage) {
		notificationService = new NotificationService(stage);
		notificationService.popupLifeTime().bind(settings.popupLifetime());

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
				task.setOnSucceeded(e -> notificationService.notifySuccess(
						t.getSubject()
						, task.getValue()
						, () -> {
							if (stage.isIconified()) {
								stage.setIconified(false);
							}
							if (!stage.isFocused()) {
								stage.requestFocus();
							}
						}));
				task.setOnFailed(e -> LOGGER.error("notify new thread {}", t.getId(), e.getSource().getException()));
				ThreadPool.getDefault().submit(PoolPriority.MIN, "notify new thread", task);
			});

			return null;
		});
	}

	private void initGmail() {
		gmail = new GmailService(App.MAIL_CLIENT);
	}

	private void initProxy() {
		if (settings.proxyEnabled().getValue()) {
			System.setProperty("http.proxyHost", settings.proxyHost().get());
			System.setProperty("http.proxyPort", "" + settings.proxyPort().get());
			System.setProperty("https.proxyHost", settings.proxyHost().get());
			System.setProperty("https.proxyPort", "" + settings.proxyPort().get());
			if (settings.proxyBasicAuth().get()) {
				System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
			}
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return getRequestorType() == RequestorType.PROXY
							&& getRequestingHost().equals(settings.proxyHost().get())
							&& getRequestingPort() == settings.proxyPort().get()
							? new PasswordAuthentication(
									settings.proxyUser().get(),
									settings.proxyPasswd().get().toCharArray())
									: null;
				}
			});
		}
	}
}
