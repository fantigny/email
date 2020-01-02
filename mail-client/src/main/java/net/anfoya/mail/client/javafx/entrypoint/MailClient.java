package net.anfoya.mail.client.javafx.entrypoint;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.PlatformUtil;

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
import net.anfoya.java.util.concurrent.ObservableExecutors;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.javafx.notification.NotificationService;
import net.anfoya.javafx.util.WinShell32;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.MailBrowser.Mode;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.settings.VersionHelper;
import net.anfoya.mail.browser.javafx.util.UrlHelper;
import net.anfoya.mail.client.App;
import net.anfoya.mail.client.javafx.MailChoice;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.model.Contact;
import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Section;
import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceInfo;

public class MailClient extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailClient.class);

	private static final String OPTION_LOG = "option: {} = {}";
	private static final String[][] OPTIONS = {
			{ "jdk.gtk.version"						, "2.2"		},	// uses GTK2 lib for drag'n drop compatibility (Linux)
			{ "glass.accessible.force"				, "false"	}, 	// disabled to avoid crash on close (macOs)
			{ "sun.net.http.allowRestrictedHeaders"	, "true"	}	// allows restricted headers for Google sign in
	};

	///////////////////
	///////////////////
	///////////////////

	public static void main(final String[] args) {
		if (PlatformUtil.isWindows()) {
			WinShell32.setExplicitAppUserModelId(App.getName());
			LOGGER.info("app user model id is set to {}", WinShell32.getCurrentProcessExplicitAppUserModelID());
		}

		Arrays.stream(OPTIONS).forEach(o -> System.setProperty(o[0], o[1]));
		System.getProperties().forEach((k, v) -> LOGGER.info(OPTION_LOG, k, v));

		launch(args);
	}

	///////////////////
	///////////////////
	///////////////////


	private MailService<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> mailService;
	private NotificationService notificationService;
	private Settings settings;

	@Override
	public void init() throws Exception {
		Platform.setImplicitExit(false);

		initThreadPool();
		initSettings();
		initProxy();
	}

	@Override
	public void start(final Stage primaryStage) {
		primaryStage.initStyle(StageStyle.DECORATED);

		final MailServiceInfo info;
		if (settings.mailServiceInfo().isNotNull().get()) {
			info = settings.mailServiceInfo().get();
		} else {
			info = new MailChoice().getMailServiceInfo();
			if (info == null) {
				LOGGER.error("no mail provider selected");
				Platform.exit();
				return;
			}
		}

		mailService = info.getMailService(App.getName());
		mailService.setOnAuth(() -> {
			settings.mailServiceInfo().set(info);
			settings.saveNow();
			Platform.runLater(() -> {
				initNewThreadNotifier(primaryStage);
				showBrowser(primaryStage);
				checkVersion();
			});
		});
		mailService.setOnAuthFailed(() -> {
			settings.mailServiceInfo().set(null);
			LOGGER.error(mailService.getAuthException().getMessage());
			restart();
		});

		mailService.authenticate();
	}

	protected void restart() {
		start(new Stage());
	}

	@Override
	public void stop() throws Exception {
		super.stop();

		settings.saveNow();
	}

	private boolean confirmClose(final Stage stage) {
		if (settings.confirmOnQuit().get()) {
			final CheckBox checkBox = new CheckBox("don't show again");
			checkBox.selectedProperty().addListener((ov, o, n) -> settings.confirmOnQuit().set(!n));

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.initOwner(stage);
			alert.setTitle("FisherMail");
			alert.setHeaderText("you are about to close FisherMail\rnew e-mail notification will be stopped");
			alert.getDialogPane().contentProperty().set(checkBox);
			return alert
					.showAndWait()
					.filter(response -> response == ButtonType.OK)
					.isPresent();
		}

		return true;
	}

	private boolean confirmSignout(Stage stage) {
		if (settings.confirmOnSignout().get()) {
			final CheckBox checkBox = new CheckBox("don't show again");
			checkBox.selectedProperty().addListener((ov, o, n) -> settings.confirmOnSignout().set(!n));

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.initOwner(stage);
			alert.setTitle("FisherMail");
			alert.setHeaderText("sign out from your e-mail account\rthis will close the mail browser");
			alert.getDialogPane().contentProperty().set(checkBox);
			return alert
					.showAndWait()
					.filter(r -> r == ButtonType.OK)
					.isPresent();
		}

		return true;
	}

	private void initThreadPool() {
		ThreadPool.setDefault(
				ObservableExecutors.newCachedThreadPool("min", java.lang.Thread.MIN_PRIORITY)
				,  null
				,  ObservableExecutors.newCachedThreadPool("max", java.lang.Thread.MAX_PRIORITY));
	}

	private void initSettings() {
		settings = new Settings(mailService);
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
		MailBrowser<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> mailBrowser;
		try {
			mailBrowser = new MailBrowser<>(mailService, notificationService, settings);
		} catch (final MailException e) {
			LOGGER.error("load mail browser", e);
			return;
		}
		mailBrowser.addOnModeChange(() -> refreshTitle(stage, mailBrowser));
		mailBrowser.setOnSignout(() -> {
			if (!confirmSignout(stage)) {
				return;
			}
			stage.hide();
			mailService.signout();
			settings.mailServiceInfo().set(null);
			settings.saveNow();

			restart();
		});

		stage.getIcons().add(App.getIcon());
		stage.setScene(mailBrowser);
		stage.setOnCloseRequest(e -> {
			if (!confirmClose(stage)) {
				e.consume();
			} else {
				Platform.exit();
			}
		});
		stage.setOnHiding(e -> ThreadPool.getDefault().mustRun("save global settings", () -> {
			mailService.stopListening();
			mailBrowser.saveSettings();
			settings.windowX().set(stage.getX());
			settings.windowY().set(stage.getY());
			settings.windowWidth().set(stage.getWidth());
			settings.windowHeight().set(stage.getHeight());
		}));

		initSize(stage);
		stage.show();

		mailService.startListening();
	}

	private void refreshTitle(final Stage stage, MailBrowser<?, ?, ?, ?, ?> browser) {
		final Task<String> titleTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				final Mode mode = browser.modeProperty().get();
				final Contact contact = mailService.getContact();
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

		mailService.addOnNewMessage(threads -> {
			LOGGER.info("notify new thread");
			new AudioClip(Settings.MP3_NEW_MAIL).play();
			threads.forEach(t -> {
				final Task<String> task = new Task<String>() {
					@Override
					protected String call() throws Exception {
						final Message m = mailService.getMessage(t.getLastMessageId());
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
