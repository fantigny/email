package net.anfoya.mail.browser.javafx.entrypoint;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class CssTest extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		final SplitPane splitPane = new SplitPane();
		splitPane.setOpacity(0);

		final Scene scene = new Scene(splitPane, 800, 600);

		primaryStage.initStyle(StageStyle.UNIFIED);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
}