package test.focus;

import com.sun.javafx.scene.web.skin.HTMLEditorSkin;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class FocusTest extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		final HTMLEditor editor = new HTMLEditor();
		final WebView view = (WebView) ((GridPane)((HTMLEditorSkin)editor.getSkin()).getChildren().get(0)).getChildren().get(2);

		primaryStage.setScene(new Scene(editor));
		primaryStage.sizeToScene();
		primaryStage.show();

		Platform.runLater(() -> {
		    view.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100, 200, 200, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null));
		    editor.requestFocus();
		    view.fireEvent(new MouseEvent(MouseEvent.MOUSE_RELEASED, 100, 100, 200, 200, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null));
		});
	}

}
