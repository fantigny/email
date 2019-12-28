package net.anfoya.mail.client;

import javafx.scene.image.Image;

public class App {
	private static final String MAIL_CLIENT = "FisherMail";

	private static final Image icon;

	static {
		icon = new Image(App.class.getResourceAsStream("/net/anfoya/mail/img/Mail.png"));
	}

	public static String getName() {
		return MAIL_CLIENT;
	}

	public static Image getIcon() {
		return icon;
	}
}
