package net.anfoya.downloads.javafx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import net.anfoya.downloads.javafx.allocine.AllocineField;

public class SearchPane extends BorderPane {
	private final Label label;
	private final AllocineField text;
	private final Button button;

	public SearchPane() {
		setPadding(new Insets(5));

		label = new Label("Title ");
		setAlignment(label, Pos.CENTER);
		setLeft(label);

		text = new AllocineField();
		text.prefWidthProperty().bind(widthProperty());
		setMargin(text, new Insets(0, 3, 0, 3));
		setCenter(text);

		button = new Button("Search");
		button.disableProperty().bind(text.valueProperty().asString().isEmpty());
		setRight(button);
	}

	public void setOnSearchAction(final Callback<String[], Void> callback) {
		text.setOnSearch(callback);
		button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				text.fireEvent(new KeyEvent(button, text, KeyEvent.KEY_PRESSED, "\r", "ENTER", KeyCode.ENTER, false, false, false, false));
			}
		});
	}

	public Callback<String, Void> getSearchedCallBack() {
		return new Callback<String, Void>() {
			@Override
			public Void call(final String search) {
				setSearched(search);
				return null;
			}
		};
	}

	public void setSearched(final String search) {
		text.setSearchedText(search);
	}
}
