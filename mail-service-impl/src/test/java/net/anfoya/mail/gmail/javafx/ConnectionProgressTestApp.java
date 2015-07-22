package net.anfoya.mail.gmail.javafx;

import javafx.application.Application;
import javafx.stage.Stage;

public class ConnectionProgressTestApp extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		final ConnectionProgress cp = new ConnectionProgress();
		cp.setValue(.5, "test progress");
	}
}
