package net.anfoya.mail.client.javafx.entrypoint;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailClient extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailClient.class);

	public static void main(final String[] args) {
		launch(args);
	}

	private MailService<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> mailService;

	@Override
	public void init() {
		Settings.load();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.initStyle(StageStyle.UNIFIED);
		primaryStage.setOnCloseRequest(e -> ThreadPool.getInstance().shutdown());

		MailBrowser<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> mailBrowser;
		do {
			try {
				mailService = new GmailService();
				mailService.connect("net.anfoya.mail-client");
			} catch (final MailException e) {
				LOGGER.error("login error", e);
				System.exit(1);
			}
			mailBrowser = new MailBrowser<Section, Tag, Thread, Message, Contact>((MailService<Section, Tag, Thread, Message, Contact>) mailService);
			mailBrowser.showAndWait();
		} while (!mailBrowser.isQuit());
	}
}
