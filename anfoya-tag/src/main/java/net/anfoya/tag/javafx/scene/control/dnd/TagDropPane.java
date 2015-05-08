package net.anfoya.tag.javafx.scene.control.dnd;

import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagService;
import net.anfoya.tag.service.TagServiceException;

public class TagDropPane<S extends SimpleSection, T extends SimpleTag> extends GridPane {
	private final TagService<S, T> tagService;

	public TagDropPane(final TagService<S, T> tagService) {
		this.tagService = tagService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(100);

		final HBox removeBox = new HBox(0, new Label("remove"));
		removeBox.setAlignment(Pos.CENTER);
		removeBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		removeBox.setPrefWidth(200);
		removeBox.setPrefHeight(50);
		removeBox.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				removeBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: red");
				event.consume();
			}
		});
		removeBox.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				removeBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
				event.consume();
			}
		});
		removeBox.setOnDragOver(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		removeBox.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) event.getDragboard().getContent(DndFormat.TAG_DATA_FORMAT);
				remove(tag);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final HBox renameBox = new HBox(0, new Label("rename"));
		renameBox.setAlignment(Pos.CENTER);
		renameBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		renameBox.setPrefWidth(200);
		renameBox.setPrefHeight(50);
		renameBox.setOnDragEntered(event -> {
			renameBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: red");
			event.consume();
		});
		renameBox.setOnDragExited(event -> {
			renameBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
			event.consume();
		});
		renameBox.setOnDragOver(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		renameBox.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) event.getDragboard().getContent(DndFormat.TAG_DATA_FORMAT);
				rename(tag);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		addRow(0, renameBox, removeBox);

		final HBox newSectionBox = new HBox(0, new Label("new section"));
		newSectionBox.setAlignment(Pos.CENTER);
		newSectionBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		newSectionBox.setPrefWidth(200);
		newSectionBox.setPrefHeight(50);
		newSectionBox.setOnDragExited(event -> {
			newSectionBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
			event.consume();
		});
		newSectionBox.setOnDragOver(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		newSectionBox.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) event.getDragboard().getContent(DndFormat.TAG_DATA_FORMAT);
				newSection(tag);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		setColumnSpan(newSectionBox, 2);
		addRow(1, newSectionBox);
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
			tagService.moveToSection(section, tag);
		} catch (final TagServiceException e) {
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
		} catch (final TagServiceException e) {
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
			} catch (final TagServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
