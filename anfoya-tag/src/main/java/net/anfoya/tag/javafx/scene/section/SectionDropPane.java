package net.anfoya.tag.javafx.scene.section;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.tag.service.Section;
import net.anfoya.tag.service.Tag;
import net.anfoya.tag.service.TagService;

public class SectionDropPane<S extends Section> extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(SectionDropPane.class);

	private final TagService<S, ? extends Tag> tagService;
	private final UndoService undoService;

	private EventHandler<ActionEvent> updateHandler;

	public SectionDropPane(final TagService<S, ? extends Tag> tagService, UndoService undoService) {
		getStyleClass().add("droparea-grid");
		this.tagService = tagService;
		this.undoService = undoService;

		final DropArea removeArea = new DropArea("remove", Section.SECTION_DATA_FORMAT);
		removeArea.<S>setDropCallback((S s) -> remove(s));

		final DropArea renameArea = new DropArea("rename", Section.SECTION_DATA_FORMAT);
		renameArea.<S>setDropCallback((S s) -> rename(s));

		final DropArea hideArea = new DropArea("hide", Section.SECTION_DATA_FORMAT);
		hideArea.<S>setDropCallback((S s) -> hide(s));

		int row = 0;
		addRow(row++, renameArea);
		setColumnSpan(renameArea, 2);
		setHgrow(renameArea, Priority.ALWAYS);
		setVgrow(renameArea, Priority.ALWAYS);

		addRow(row++, hideArea, removeArea);
		setHgrow(hideArea, Priority.ALWAYS);
		setVgrow(hideArea, Priority.ALWAYS);
		setHgrow(removeArea, Priority.ALWAYS);
		setVgrow(removeArea, Priority.ALWAYS);

		setMaxHeight(65 * row);
	}

	public void setOnUpdateSection(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	private Void rename(final S section) {
		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog(section.getName());
			inputDialog.setTitle("FisherMail");
			inputDialog.setHeaderText("rename \"" + section.getName() + "\"");
			inputDialog.setContentText("new name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return null;
			}
			name = response.get();
			if (name.length() < 3) {
				final Alert errorDialog = new Alert(AlertType.ERROR);
				errorDialog.setTitle("FisherMail");
				errorDialog.setHeaderText("name is too short \"" + name + "\"");
				errorDialog.setContentText("section should be a least 3 letters long.");
				errorDialog.showAndWait();
				name = "";
			}
		}

		final String finalAnswer = name;
		final String description = "rename section " + section.getName() + " to " + finalAnswer;
		final Task<S> task = new Task<S>() {
			@Override
			protected S call() throws Exception {
				return tagService.rename(section, finalAnswer);
			}
		};
		task.setOnSucceeded(event -> {
			undoService.setUndo(() -> tagService.rename(task.get(), section.getName()), "rename " + section.getName());
			updateHandler.handle(null);
		});
		task.setOnFailed(e -> LOGGER.error(description, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, description, task);

		return null;
	}

	private Void remove(final S section) {
		final Alert warningDialog = new Alert(AlertType.WARNING, "", ButtonType.OK, ButtonType.CANCEL);
		warningDialog.setTitle("FisherMail");
		warningDialog.setHeaderText("remove \"" + section.getName() + "\"?");
		warningDialog.setContentText("can't be undone");
		warningDialog.showAndWait()
			.filter(r -> r == ButtonType.OK)
			.ifPresent(r -> {
				final Task<Void> task = new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						tagService.remove(section);
						return null;
					}
				};
				task.setOnSucceeded(event -> updateHandler.handle(null));
				task.setOnFailed(e -> LOGGER.error("remove section {}", section.getName(), e.getSource().getException()));
				ThreadPool.getDefault().submit(PoolPriority.MAX, "remove section " + section.getName(), task);
			});

		return null;
	}

	private Void hide(final S section) {
		final String description = "hide " + section.getName();
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				tagService.hide(section);
				return null;
			}
		};
		task.setOnSucceeded(event -> {
			undoService.setUndo(
					() -> tagService.show(section)
					, description);
			updateHandler.handle(null);
		});
		task.setOnFailed(e -> LOGGER.error(description, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, description, task);

		return null;
	}
}
