package net.anfoya.javafx.scene.dnd;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class DropArea extends HBox {
	private final DataFormat dataFormat;

	public DropArea(final String name, final DataFormat dataFormat) {
		super(new Label(name));
		setAlignment(Pos.CENTER);

		getStyleClass().add("dropbutton-box");

		setOnDragOver(e -> {
			if (e.getDragboard().hasContent(dataFormat)) {
				e.acceptTransferModes(TransferMode.LINK);
			}
			e.consume();
		});
		setOnDragEntered(e -> {
			if (e.getDragboard().hasContent(dataFormat)) {
				getStyleClass().add("dropbutton-box-hover");
			}
			e.consume();
		});
		setOnDragExited(e -> getStyleClass().remove("dropbutton-box-hover"));

		this.dataFormat = dataFormat;
	}

	public void setDropHandler(EventHandler<ActionEvent> handler) {
		setOnDragDropped(e -> {
			if (e.getDragboard().hasContent(dataFormat)) {
				handler.handle(null);
				e.setDropCompleted(true);
				e.consume();
			}
		});
	}

	public <T> void setDropCallback(Callback<T, Void> callback) {
		setOnDragDropped(e -> {
			if (e.getDragboard().hasContent(dataFormat)) {
				@SuppressWarnings("unchecked")
				final T t = (T) e.getDragboard().getContent(dataFormat);
				callback.call(t);
				e.setDropCompleted(true);
				e.consume();
			}
		});
	}

}
