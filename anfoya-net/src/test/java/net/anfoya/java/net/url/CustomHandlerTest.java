package net.anfoya.java.net.url;

import java.net.URL;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class CustomHandlerTest extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		final WebView view = new WebView();
		view.getEngine().setCreatePopupHandler(f -> null);
		final Scene scene = new Scene(view, 800, 600);
		primaryStage.initStyle(StageStyle.UNIFIED);
		primaryStage.setScene(scene);
		primaryStage.show();

		URL.setURLStreamHandlerFactory(new CustomHandlerFactory(null));
//		view.getEngine().load("http://www.w3schools.com/html/tryit.asp?filename=tryhtml_intro");
//		view.getEngine().load("http://www.dvdrip-fr.com/Site/fiche.php?id=4867");
//		view.getEngine().load("https://pirateproxy.sx/browse");
		view.getEngine().load("https://pirateproxy.sx");
	}
}