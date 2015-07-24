package net.anfoya.javafx.scene.dnd;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

public class DropArea extends HBox {
	public DropArea(final String name, final DataFormat dataFormat) {
		super(new Label(name));
		setAlignment(Pos.CENTER);

		getStyleClass().add("dropbutton-box");

		setOnDragEntered(e -> {
			if (e.getDragboard().hasContent(dataFormat)) {
				getStyleClass().add("dropbutton-box-hover");
				e.consume();
			}
		});
		setOnDragExited(e -> {
			getStyleClass().remove("dropbutton-box-hover");
		});
		setOnDragOver(e -> {
			if (e.getDragboard().hasContent(dataFormat)) {
				e.acceptTransferModes(TransferMode.ANY);
				e.consume();
			}
		});
	}
}
