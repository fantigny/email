package net.anfoya.tag.javafx.scene.dnd;

import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.GridPane;
import net.anfoya.javafx.scene.dnd.DndPaneTranslationHelper;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.tag.service.Tag;

public class ExtItemDropPane<T extends Tag> extends GridPane {
	public ExtItemDropPane(final DataFormat extItemDataFormat) {
		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(100);

		new DndPaneTranslationHelper(this);

		final DropArea newTagArea = new DropArea("add new tag", extItemDataFormat);
		newTagArea.setOnDragDropped(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasContent(extItemDataFormat)) {
				final String name = getName();
				if (name == null) {
					e.consume();
					return;
				}

				final ClipboardContent content = new ClipboardContent();
				content.put(extItemDataFormat, db.getContent(extItemDataFormat));
				content.put(DndFormat.TAG_NAME_DATA_FORMAT, name);
				db.setContent(content);
			}
		});

		addRow(0, newTagArea);
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
