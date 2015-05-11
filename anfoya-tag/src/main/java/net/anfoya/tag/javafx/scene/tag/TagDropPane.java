package net.anfoya.tag.javafx.scene.tag;

import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

public class TagDropPane<S extends SimpleSection, T extends SimpleTag> extends GridPane {
	private final TagService<S, T> tagService;

	public TagDropPane(final TagService<S, T> tagService) {
		this.tagService = tagService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(100);

		final DropArea removeArea = new DropArea("remove", DndFormat.TAG_DATA_FORMAT);
		removeArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) event.getDragboard().getContent(DndFormat.TAG_DATA_FORMAT);
				remove(tag);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea renameArea = new DropArea("rename", DndFormat.TAG_DATA_FORMAT);
		renameArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) event.getDragboard().getContent(DndFormat.TAG_DATA_FORMAT);
				rename(tag);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		addRow(0, renameArea, removeArea);

		final DropArea newSectionArea = new DropArea("new section", DndFormat.TAG_DATA_FORMAT);
		newSectionArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) event.getDragboard().getContent(DndFormat.TAG_DATA_FORMAT);
				newSection(tag);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		setColumnSpan(newSectionArea, 2);
		addRow(1, newSectionArea);
	}

	private void newSection(final T tag) {
		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog();
			inputDialog.setTitle("Create new section");
			inputDialog.setHeaderText("");
			inputDialog.setContentText("Section name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return;
			}
			name = response.get();
			if (name.length() < 3) {
				final Alert alertDialog = new Alert(AlertType.ERROR);
				alertDialog.setTitle("Create new section");
				alertDialog.setHeaderText("Section name is too short: " + name);
				alertDialog.setContentText("Section name should be a least 3 letters long.");
				alertDialog.showAndWait();
				name = "";
			}
		}

		try {
			final S section = tagService.addSection(name);
			tagService.moveToSection(tag, section);
		} catch (final TagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void rename(final T tag) {
		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog(tag.getName());
			inputDialog.setTitle("Rename tag");
			inputDialog.setHeaderText("");
			inputDialog.setContentText("Tag name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return;
			}
			name = response.get();
			if (name.length() < 3) {
				final Alert alertDialog = new Alert(AlertType.ERROR);
				alertDialog.setTitle("Rename tag");
				alertDialog.setHeaderText("Tag name is too short: " + name);
				alertDialog.setContentText("Tag name should be a least 3 letters long.");
				alertDialog.showAndWait();
				name = "";
			}
		}

		try {
			tagService.rename(tag, name);
		} catch (final TagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void remove(final T tag) {
		final Alert confirmDialog = new Alert(AlertType.CONFIRMATION, "", new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
		confirmDialog.setTitle("Remove tag");
		confirmDialog.setHeaderText("Remove tag: \"" + tag.getName() + "\"?");
		confirmDialog.setContentText("");
		final Optional<ButtonType> response = confirmDialog.showAndWait();
		if (response.isPresent() && response.get() == ButtonType.OK) {
			try {
				tagService.remove(tag);
			} catch (final TagException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
