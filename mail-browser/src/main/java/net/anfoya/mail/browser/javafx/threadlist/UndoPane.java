package net.anfoya.mail.browser.javafx.threadlist;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

public class UndoPane extends GridPane {
	private final Button undoButton;

	public UndoPane() {
		getStyleClass().add("droparea-grid");
		setMaxHeight(65);

		undoButton = new Button("undo");
		undoButton.getStyleClass().add("dropbutton-box");
		undoButton.prefWidthProperty().bind(widthProperty());
		undoButton.prefHeightProperty().bind(heightProperty());
		add(undoButton, 0, 0);
	}

	public void setOnUndo(EventHandler<ActionEvent> handler) {
		undoButton.setOnAction(handler);
	}
}
