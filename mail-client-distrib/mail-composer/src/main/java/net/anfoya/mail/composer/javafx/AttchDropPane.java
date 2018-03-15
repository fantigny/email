package net.anfoya.mail.composer.javafx;

import java.io.File;

import javafx.scene.input.DataFormat;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import net.anfoya.javafx.scene.dnd.DropArea;

public class AttchDropPane extends GridPane {
	public static final DataFormat FILE_DATA_FORMAT = new DataFormat("DND_REMOVE_FILE_DATA_FORMAT");

	private final DropArea removeArea;

	public AttchDropPane() {
		setMaxHeight(50);
		getStyleClass().add("droparea-grid");

		removeArea = new DropArea("remove", FILE_DATA_FORMAT);

		addRow(0, removeArea);
		setHgrow(removeArea, Priority.ALWAYS);
		setVgrow(removeArea, Priority.ALWAYS);
	}

	public void setOnRemove(Callback<File, Void> callback) {
		removeArea.<File>setDropCallback(callback);
	}
}
