package net.anfoya.mail.client;

import javafx.scene.image.Image;

public class App {
	private static final String NAME = "FisherMail";

	private static final Image icon;

	static {
		icon = new Image(App.class.getResourceAsStream("/net/anfoya/mail/img/Mail.png"));
	}

	public static String getName() {
		return NAME;
	}

	public static Image getIcon() {
		return icon;
	}
}
