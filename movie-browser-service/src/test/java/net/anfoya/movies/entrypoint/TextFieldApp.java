package net.anfoya.movies.entrypoint;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class TextFieldApp extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage stage) throws Exception {
		final BorderPane pane = new BorderPane();
		final TextField textField = new TextField("test");
		pane.setTop(textField);
		final Scene scene = new Scene(pane, 640, 480);
		stage.setScene(scene);
		stage.show();
	}
}
