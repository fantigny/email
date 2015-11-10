package net.anfoya.tag.javafx.scene.dnd;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import net.anfoya.javafx.scene.dnd.DndPaneTranslationHelper;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.tag.service.Tag;

public class ExtItemDropPane<T extends Tag> extends GridPane {
	public static final DataFormat ADD_TAG_DATA_FORMAT = new DataFormat("ADD_TAG_DATA_FORMAT");
	public static final DataFormat TAG_NAME_DATA_FORMAT = new DataFormat("TAG_NAME_DATA_FORMAT");

	public ExtItemDropPane() {
		setMaxHeight(50);
		getStyleClass().add("droparea-grid");
		new DndPaneTranslationHelper(this);

		final DropArea newTagArea = new DropArea("add new tag", ADD_TAG_DATA_FORMAT);
		newTagArea.setOnDragDropped(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasContent(ADD_TAG_DATA_FORMAT)) {
				final String name = getName();
				if (name == null) {
					e.setDropCompleted(false);
				} else {
					final ClipboardContent content = new ClipboardContent();
					content.put(TAG_NAME_DATA_FORMAT, name);
					db.setContent(content);
					e.setDropCompleted(true);
				}
				e.consume();
			}
		});

		addRow(0, newTagArea);
		setHgrow(newTagArea, Priority.ALWAYS);
		setVgrow(newTagArea, Priority.ALWAYS);
	}

	private String getName() {
		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog();
			inputDialog.setTitle("Create new tag");
			inputDialog.setHeaderText("");
			inputDialog.setContentText("Tag name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return null;
			}
			name = response.get();
			if (name.length() < 3) {
				final Alert alertDialog = new Alert(AlertType.ERROR);
				alertDialog.setTitle("Create new tag");
				alertDialog.setHeaderText("Section name is too short: " + name);
				alertDialog.setContentText("Section name should be a least 3 letters long.");
				alertDialog.showAndWait();
				name = "";
			}
		}

		return name;
	}
}
