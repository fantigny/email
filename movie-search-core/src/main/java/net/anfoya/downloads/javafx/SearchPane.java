package net.anfoya.downloads.javafx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import net.anfoya.downloads.javafx.allocine.QuickSearchField;
import net.anfoya.movie.connector.QuickSearchVo;

public class SearchPane extends BorderPane {
	private final QuickSearchField text;
	private final Button button;

	public SearchPane() {
		setPadding(new Insets(5));

		text = new QuickSearchField();
		text.prefWidthProperty().bind(widthProperty());
		setMargin(text, new Insets(0, 3, 0, 3));
		setCenter(text);

		button = new Button("Search");
		button.disableProperty().bind(text.valueProperty().asString().isEmpty());
		setRight(button);
	}

	public void setOnSearchAction(final Callback<QuickSearchVo, Void> callback) {
		text.setOnSearch(callback);
		button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				text.getEditor().fireEvent(new KeyEvent(button, text, KeyEvent.KEY_PRESSED, "\r", "ENTER", KeyCode.ENTER, false, false, false, false));
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
