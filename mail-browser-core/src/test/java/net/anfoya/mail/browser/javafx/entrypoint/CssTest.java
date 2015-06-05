package net.anfoya.mail.browser.javafx.entrypoint;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class CssTest extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		final WebView view = new WebView();

		final SplitPane splitPane = new SplitPane();
		splitPane.getItems().add(view);

//		splitPane.setOpacity(0);

		final Scene scene = new Scene(splitPane, 800, 600);

		primaryStage.initStyle(StageStyle.UNIFIED);
		primaryStage.setScene(scene);
		primaryStage.show();

		view.getEngine().load("http://www.dvdrip-fr.com/Site/fiche.php?id=4867");
	}
}