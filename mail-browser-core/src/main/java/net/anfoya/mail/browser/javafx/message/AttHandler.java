package net.anfoya.mail.browser.javafx.message;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class AttHandler {

	public void start(final String name, final String messageId) {
		final Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText(name);
		alert.setContentText(messageId);
		alert.showAndWait();
	}
}
