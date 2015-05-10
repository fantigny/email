package net.anfoya.tag.javafx.scene.dnd;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

public class DropArea extends HBox {
	public DropArea(final String name, final DataFormat dataFormat) {
		super(0, new Label(name));
		setAlignment(Pos.CENTER);
		setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		setPrefWidth(400);
		setPrefHeight(50);
		setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(dataFormat)) {
				setStyle("-fx-background-color: #DDDDDD; -fx-border-color: red");
				event.consume();
			}
		});
		setOnDragExited(event -> {
			if (event.getDragboard().hasContent(dataFormat)) {
				setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
				event.consume();
			}
		});
		setOnDragOver(event -> {
			if (event.getDragboard().hasContent(dataFormat)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
	}
}
