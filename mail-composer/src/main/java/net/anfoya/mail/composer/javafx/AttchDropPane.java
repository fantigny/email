package net.anfoya.mail.composer.javafx;

import java.io.File;

import javafx.scene.input.DataFormat;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import net.anfoya.javafx.scene.dnd.DropArea;

public class AttchDropPane extends GridPane {
	public static final DataFormat FILE_DATA_FORMAT = new DataFormat("DND_REMOVE_FILE_DATA_FORMAT");

	private Callback<File, Void> removeCallback;

	public AttchDropPane() {
		setMaxHeight(50);
		getStyleClass().add("droparea-grid");

		final DropArea removeArea = new DropArea("remove", FILE_DATA_FORMAT);
		removeArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(FILE_DATA_FORMAT)) {
				removeCallback.call((File)event.getDragboard().getContent(FILE_DATA_FORMAT));
				event.setDropCompleted(true);
			} else {
				event.setDropCompleted(false);
			}
			event.consume();
		});

		addRow(0, removeArea);
		setHgrow(removeArea, Priority.ALWAYS);
		setVgrow(removeArea, Priority.ALWAYS);
	}

	public void setOnRemove(Callback<File, Void> callback) {
		removeCallback = callback;
	}
}
