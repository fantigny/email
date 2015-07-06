package net.anfoya.mail.browser.javafx.entrypoint;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class CssTest extends Application {

	public static void main(final String[] args) {
		launch(args);
	}


	private Stage stage;

	@Override
	public void start(final Stage stage) throws Exception {
		this.stage = stage;

		stage.initStyle(StageStyle.UNDECORATED);
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(3));

		final Scene scene = new Scene(mainPane, 800, 600);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/mail/css/Mail.css").toExternalForm());

		final Button minButton = new Button("_");
		minButton.setOnAction(e -> stage.setIconified(true));

		final Button maxButton = new Button("+");
		maxButton.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
		stage.maximizedProperty().addListener((ov, o, n) -> maxButton.setText(n? "-": "+"));

		final Button closeButton = new Button("x");
		closeButton.setOnAction(e -> stage.close());

		final ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png")));
		icon.setFitHeight(16);
		icon.setFitWidth(icon.getFitHeight());

		final Label title = new Label("title");
		title.setGraphic(icon);
		title.setOnMouseClicked(e -> doubleClickMax(e));
		title.setOnMouseDragged(e -> dragMove(e));

		final HBox filler = new HBox();
		filler.setOnMouseClicked(e -> doubleClickMax(e));
		HBox.setHgrow(filler, Priority.ALWAYS);

		final HBox sysTools = new HBox(title, filler, minButton, maxButton, closeButton);
		sysTools.setAlignment(Pos.CENTER);
		mainPane.setTop(sysTools);

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

	private void dragMove(final MouseEvent e) {
        if (!e.isPrimaryButtonDown()) {
            return;
        }
        if (stage.isFullScreen()) {
            return;
        }
        /*
         * Long press generates drag event!
         */
        if (e.isStillSincePress()) {
            return;
        }
        if (stage.isMaximized()) {
            // Remove maximized state
            stage.setMaximized(false);
            return;
        }

	}

	private void doubleClickMax(final MouseEvent e) {
		if (e.getClickCount() > 1) {
			stage.setMaximized(!stage.isMaximized());
		}
	}
}