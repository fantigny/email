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
import net.anfoya.mail.service.MailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailClient extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailClient.class);

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

		MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> mailBrowser;
		do {
			MailService<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> mailService = null;
			try {
				mailService = new GmailService();
				mailService.connect("net.anfoya.mail-client");
			} catch (final MailException e) {
				LOGGER.error("login failed, {}", e.getMessage());
				LOGGER.debug("full stack", e);
				System.exit(0);
			}
			mailBrowser = new MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact>(mailService);
			mailBrowser.showAndWait();
		} while (!mailBrowser.isQuit());
	}
}
