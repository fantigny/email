package net.anfoya.mail.client.javafx.entrypoint;

import javafx.application.Application;
import javafx.stage.Stage;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.client.App;
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

	private GmailService gmail;

	@Override
	public void init() throws Exception {
		Settings.load();
		gmail = new GmailService();
		try {
			gmail.connect(App.MAIL_CLIENT);
		} catch (final MailException e) {
			throw new Exception("login failed", e);
		}
		if (gmail.disconnected().get()) {
			System.exit(0);
		}
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		for(;;) {
			if (!gmail.disconnected().get()) {
				final MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> mailBrowser =
						new MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact>(gmail);
				mailBrowser.showAndWait();
				if (mailBrowser.isQuit()) {
					break;
				}
			}
			try {
				gmail.connect(App.MAIL_CLIENT);
			} catch (final MailException e) {
				throw new Exception("login failed", e);
			}
			if (gmail.disconnected().get()) {
				break;
			}
		}
	}
}
