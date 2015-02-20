package net.anfoya.downloads.javafx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class SearchPane extends BorderPane {
	private final Label label;
	private final TextField text;
	private final Button button;

	public SearchPane() {
		setPadding(new Insets(5));

		label = new Label("Title ");
		setAlignment(label, Pos.CENTER);
		setLeft(label);

		text = new TextField();
		setMargin(text, new Insets(0, 3, 0, 3));
		setCenter(text);

		button = new Button("Search");
		button.disableProperty().bind(text.textProperty().isEmpty());
		setRight(button);
	}

	public void setOnSearchAction(final EventHandler<ActionEvent> listener) {
		text.setOnAction(listener);
		button.setOnAction(listener);
	}

	public Callback<String, Void> getSearchCallBack() {
		return new Callback<String, Void>() {
			@Override
			public Void call(final String search) {
				text.setText(search);
				return null;
			}
		};
	}

	public String getSearch() {
		return text.getText();
	}

	public void setSearch(final String search) {
		text.setText(search);
	}
}
