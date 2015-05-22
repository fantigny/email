package net.anfoya.tag.javafx.scene.section;

import java.util.Optional;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagService;

public class SectionDropPane<S extends SimpleSection> extends GridPane {

	private final TagService<S, ? extends SimpleTag> tagService;
	private EventHandler<ActionEvent> updateHandler;

	public SectionDropPane(final TagService<S, ? extends SimpleTag> tagService) {
		this.tagService = tagService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(60);

		final DropArea removeArea = new DropArea("remove", DndFormat.SECTION_DATA_FORMAT);
		removeArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DndFormat.SECTION_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final S section = (S) event.getDragboard().getContent(DndFormat.SECTION_DATA_FORMAT);
				remove(section);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea renameArea = new DropArea("rename", DndFormat.SECTION_DATA_FORMAT);
		renameArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DndFormat.SECTION_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final S section = (S) event.getDragboard().getContent(DndFormat.SECTION_DATA_FORMAT);
				rename(section);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		addRow(0, renameArea, removeArea);
	}

	private void rename(final S section) {
		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog(section.getName());
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

		final String finalAnswer = name;
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				tagService.rename(section, finalAnswer);
				return null;
			}
		};
		task.setOnFailed(event -> event.getSource().getException().printStackTrace(System.out)); // TODO Auto-generated catch block
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void remove(final S section) {
		final Alert confirmDialog = new Alert(AlertType.CONFIRMATION, "", new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
		confirmDialog.setTitle("Remove section");
		confirmDialog.setHeaderText("Remove section: \"" + section.getName() + "\"?");
		confirmDialog.setContentText("");
		final Optional<ButtonType> response = confirmDialog.showAndWait();
		if (response.isPresent() && response.get() == ButtonType.OK) {
			final Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					tagService.remove(section);
					return null;
				}
			};
			task.setOnFailed(event -> event.getSource().getException().printStackTrace(System.out)); // TODO Auto-generated catch block
			task.setOnSucceeded(event -> updateHandler.handle(null));
			ThreadPool.getInstance().submitHigh(task);
		}
	}

	public void setOnUpdateSection(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}
}
