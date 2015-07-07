package net.anfoya.mail.composer.javafx.entrypoint;

import javafx.application.Application;
import javafx.stage.Stage;
import net.anfoya.mail.client.App;
import net.anfoya.mail.composer.javafx.MessageComposer;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailContact;
import net.anfoya.mail.gmail.model.GmailMessage;

public class Ecrire extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {

		final GmailService mailService = new GmailService();
		mailService.connect(App.MAIL_CLIENT);
		new MessageComposer<GmailMessage, GmailContact>(mailService, e -> messageUpdated());
	}

	private Object messageUpdated() {
		// TODO Auto-generated method stub
		return null;
	}
}
