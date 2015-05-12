package net.anfoya.mail.browser.javafx;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class NewMail {

	public NewMail() {

        final BorderPane mainPane = new BorderPane(new Label("new mail"));

		final Stage stage = new Stage();
		stage .setTitle("FisherMail / Agaar / Agamar / Agaram");
//		stage.getIcons().add(new Image(getClass().getResourceAsStream("Mail.png")));
		stage.setScene(new Scene(mainPane, 800, 600));
        stage.show();
	}
}
