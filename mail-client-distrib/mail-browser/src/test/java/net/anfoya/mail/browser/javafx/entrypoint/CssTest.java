package net.anfoya.mail.browser.javafx.entrypoint;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;


public class CssTest extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void init() throws Exception {
		super.init();
	}

	@Override
	public void start(final Stage stage) throws Exception {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(3));

		final Scene scene = new Scene(mainPane, 800, 600);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/mail/css/Mail.css").toExternalForm());

		final ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/Mail.png")));
		icon.setFitHeight(16);
		icon.setFitWidth(icon.getFitHeight());

		final WebView view = new WebView();

		final SplitPane splitPane = new SplitPane();
		splitPane.getItems().add(view);
		mainPane.setCenter(splitPane);

		final Label a = new Label("frederic.antigny@gmail.com X");
		a.getStyleClass().add("address-label");
		a.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

		final Label b = new Label("All mail X");
		b.getStyleClass().add("address-label");
		b.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

		final Label c = new Label("my label X");
		c.getStyleClass().add("address-label");
		c.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

		mainPane.setBottom(new FlowPane(3, 3, a, b, c));

//		splitPane.setOpacity(0);

		stage.setScene(scene);
		stage.show();

//		view.getEngine().load("http://www.dvdrip-fr.com/Site/fiche.php?id=4867");
	}
}