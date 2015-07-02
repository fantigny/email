package net.anfoya.mail.client.javafx.entrypoint;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailContact;
import net.anfoya.mail.gmail.model.GmailMessage;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.service.MailException;

public class MailClient extends Application {
//	private static final Logger LOGGER = LoggerFactory.getLogger(MailClient.class);

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void init() {
		Settings.load();
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.initStyle(StageStyle.UNIFIED);
		primaryStage.setOnCloseRequest(e -> ThreadPool.getInstance().shutdown());

		final GmailService gmail = new GmailService();
		MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> mailBrowser = null;
		do {
			try {
				gmail.connect("net.anfoya.mail-client");
			} catch (final MailException e) {
				throw new Exception("login failed", e);
			}
			if (!gmail.disconnected().get()) {
				mailBrowser = new MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact>(gmail);
				mailBrowser.showAndWait();
			}
		} while (!gmail.disconnected().get() && !mailBrowser.isQuit());
	}
}
