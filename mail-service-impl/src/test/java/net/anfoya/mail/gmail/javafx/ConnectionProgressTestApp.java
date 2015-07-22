package net.anfoya.mail.gmail.javafx;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class ConnectionProgressTestApp extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	int i = 0;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		final ConnectionProgress cp = new ConnectionProgress();
		new Timer(true).schedule(new TimerTask() {
			@Override
			public void run() {
				if (++i > 100) i = 0;
				Platform.runLater(() -> cp.setValue(i / 100d, "test progress " + i + "%"));
			}
		}, 0, 200);
	}
}
