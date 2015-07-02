package net.anfoya.mail.browser.javafx.entrypoint;


import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class CssTest extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.initStyle(StageStyle.UNDECORATED);
		final BorderPane mainPane = new BorderPane();
		final Scene scene = new Scene(mainPane, 800, 600);

		final Button minButton = new Button("_");
		minButton.setOnAction(e -> primaryStage.setIconified(true));
		
		final Button maxButton = new Button("+");
		maxButton.setOnAction(e -> primaryStage.setMaximized(!primaryStage.isMaximized()));
		primaryStage.maximizedProperty().addListener((ov, o, n) -> maxButton.setText(n? "-": "+"));
		
		final Button closeButton = new Button("x");
		closeButton.setOnAction(e -> primaryStage.close());

		final ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png")));
		icon.setFitHeight(16);
		icon.setFitWidth(icon.getFitHeight());
		
		final Label title = new Label("title");
		title.setGraphic(icon);
		
		final HBox filler = new HBox();
		HBox.setHgrow(filler, Priority.ALWAYS);
		
		final HBox sysTools = new HBox(title, filler, minButton, maxButton, closeButton);
		sysTools.setAlignment(Pos.CENTER);
		mainPane.setTop(sysTools);
		
		final WebView view = new WebView();

		final SplitPane splitPane = new SplitPane();
		splitPane.getItems().add(view);
		mainPane.setCenter(splitPane);

//		splitPane.setOpacity(0);


		primaryStage.setScene(scene);
		primaryStage.show();

		view.getEngine().load("http://www.dvdrip-fr.com/Site/fiche.php?id=4867");
	}
}