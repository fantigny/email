package net.anfoya.tag.javafx.scene.tag;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import net.anfoya.javafx.scene.dnd.DndPaneTranslationHelper;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.tag.javafx.scene.section.SectionDropPane;
import net.anfoya.tag.service.Section;
import net.anfoya.tag.service.Tag;
import net.anfoya.tag.service.TagService;

public class TagDropPane<S extends Section, T extends Tag> extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(SectionDropPane.class);

	private final TagService<S, T> tagService;
	private final UndoService undoService;
	private final DndPaneTranslationHelper transHelper;

	private EventHandler<ActionEvent> updateHandler;

	private final BooleanProperty systemProperty;

	public TagDropPane(final TagService<S, T> tagService, UndoService undoService) {
		getStyleClass().add("droparea-grid");
		this.tagService = tagService;
		this.undoService = undoService;

		systemProperty = new SimpleBooleanProperty(false);
		transHelper = new DndPaneTranslationHelper(this);

		final DropArea newSectionArea = new DropArea("new section", Tag.TAG_DATA_FORMAT);
		newSectionArea.<T>setDropCallback(t -> newSection(t));
		newSectionArea.visibleProperty().bind(systemProperty.not());
		newSectionArea.managedProperty().bind(systemProperty.not());

		final DropArea renameArea = new DropArea("rename", Tag.TAG_DATA_FORMAT);
		renameArea.<T>setDropCallback(t -> rename(t));
		renameArea.visibleProperty().bind(systemProperty.not());
		renameArea.managedProperty().bind(systemProperty.not());

		final DropArea removeArea = new DropArea("remove", Tag.TAG_DATA_FORMAT);
		removeArea.<T>setDropCallback(t -> remove(t));
		removeArea.visibleProperty().bind(systemProperty.not());
		removeArea.managedProperty().bind(systemProperty.not());

		final DropArea hideArea = new DropArea("hide", Tag.TAG_DATA_FORMAT);
		hideArea.<T>setDropCallback(t -> hide(t));

		int row = 0;

		addRow(row++, newSectionArea);
		setColumnSpan(newSectionArea, 2);
		setHgrow(newSectionArea, Priority.ALWAYS);
		setVgrow(newSectionArea, Priority.ALWAYS);

		addRow(row++, renameArea, removeArea);
		setHgrow(renameArea, Priority.ALWAYS);
		setVgrow(renameArea, Priority.ALWAYS);
		setHgrow(removeArea, Priority.ALWAYS);
		setVgrow(removeArea, Priority.ALWAYS);

		addRow(row++, hideArea);
		setColumnSpan(hideArea, 2);
		setHgrow(hideArea, Priority.ALWAYS);
		setVgrow(hideArea, Priority.ALWAYS);

		setMaxHeight(65 * 3);
		removeArea.visibleProperty().addListener((ov, o, n) -> {
			setColumnSpan(hideArea, n? 2: 1);
			setMaxHeight(65 * (n? 3: 1));
		});
	}

	public void setSystem(boolean system) {
		Platform.runLater(() -> systemProperty.set(system));
	}

	private Void newSection(final T tag) {
		transHelper.reset();

		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog();
			inputDialog.setTitle("Fishermail");
			inputDialog.setHeaderText("new section");
			inputDialog.setContentText("name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return null;
			}
			name = response.get();
			if (name.length() < 3) {
				final Alert errorDialog = new Alert(AlertType.ERROR);
				errorDialog.setTitle("Fishermail");
				errorDialog.setHeaderText("name is too short \"" + name + "\"");
				errorDialog.setContentText("section should be a least 3 letters long.");
				errorDialog.showAndWait();
				name = "";
			}
		}

		final String finalAnswer = name;
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				final S section = tagService.addSection(finalAnswer);
				tagService.moveToSection(tag, section);
				return null;
			}
		};
		task.setOnSucceeded(event -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("move label {} to section {}", tag.getName(), finalAnswer, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "move label " + tag.getName() + " to section " + finalAnswer, task);

		return null;
	}

	private Void rename(final T tag) {
		transHelper.reset();

		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog(tag.getName());
			inputDialog.setTitle("FisherMail");
			inputDialog.setHeaderText("rename \"" + tag.getName() + "\"");
			inputDialog.setContentText("new name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return null;
			}
			name = response.get();
			if (name.length() < 3) {
				final Alert errorDialog = new Alert(AlertType.ERROR);
				errorDialog.setTitle("FisherMail");
				errorDialog.setHeaderText("new name is too short \"" + name + "\"");
				errorDialog.setContentText("label should be a least 3 letters long.");
				errorDialog.showAndWait();
				name = "";
			}
		}

		final String finalAnswer = name;
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				tagService.rename(tag, finalAnswer);
				return null;
			}
		};
		task.setOnSucceeded(event -> {
			undoService.setUndo(
					() -> tagService.rename(tag, tag.getName())
					, "rename " + tag.getName());
			updateHandler.handle(null);
		});
		task.setOnFailed(e -> LOGGER.error("rename {} to {}", tag.getName(), finalAnswer, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "rename " + tag.getName() + " to " + finalAnswer, task);

		return null;
	}

	private Void remove(final T tag) {
		transHelper.reset();

		final Alert warningDialog = new Alert(AlertType.WARNING, "", ButtonType.OK, ButtonType.CANCEL);
		warningDialog.setTitle("FisherMail");
		warningDialog.setHeaderText("remove \"" + tag.getName() + "\"?");
		warningDialog.setContentText("can't be undone");
		warningDialog.showAndWait()
			.filter(r -> r == ButtonType.OK)
			.ifPresent(r -> {
				final Task<Void> task = new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						tagService.remove(tag);
						return null;
					}
				};
				task.setOnSucceeded(event -> updateHandler.handle(null));
				task.setOnFailed(e -> LOGGER.error("remove {}", tag.getName(), e.getSource().getException()));
				ThreadPool.getDefault().submit(PoolPriority.MAX, "remove " + tag.getName(), task);
			});

		return null;
	}

	private Void hide(final T tag) {
		transHelper.reset();

		final String description = "hide " + tag.getName();
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				tagService.hide(tag);
				return null;
			}
		};
		task.setOnSucceeded(e -> {
			undoService.setUndo(
					() -> tagService.show(tag)
					, description);
			updateHandler.handle(null);
		});
		task.setOnFailed(e -> LOGGER.error(description, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, description, task);

		return null;
	}

	public void setOnUpdate(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}
}
