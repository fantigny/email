package net.anfoya.tag.javafx.scene.tag;

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
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.dnd.DndPaneTranslationHelper;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.tag.javafx.scene.section.SectionDropPane;
import net.anfoya.tag.service.Section;
import net.anfoya.tag.service.Tag;
import net.anfoya.tag.service.TagService;

public class TagDropPane<S extends Section, T extends Tag> extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(SectionDropPane.class);

	private final TagService<S, T> tagService;
	private final DndPaneTranslationHelper transHelper;

	private EventHandler<ActionEvent> onUpdateHandler;

	public TagDropPane(final TagService<S, T> tagService) {
		this.tagService = tagService;

		setMaxHeight(200);
		getStyleClass().add("droparea-grid");
		transHelper = new DndPaneTranslationHelper(this);

		final DropArea removeArea = new DropArea("remove", Tag.TAG_DATA_FORMAT);
		removeArea.<T>setDropCallback(t -> remove(t));

		final DropArea renameArea = new DropArea("rename", Tag.TAG_DATA_FORMAT);
		renameArea.<T>setDropCallback(t -> rename(t));

		final DropArea newSectionArea = new DropArea("new section", Tag.TAG_DATA_FORMAT);
		newSectionArea.<T>setDropCallback(t -> newSection(t));

		final DropArea hideArea = new DropArea("hide", Tag.TAG_DATA_FORMAT);
		hideArea.<T>setDropCallback(t -> hide(t));

		addRow(0, renameArea, removeArea);
		setHgrow(renameArea, Priority.ALWAYS);
		setVgrow(renameArea, Priority.ALWAYS);
		setHgrow(removeArea, Priority.ALWAYS);
		setVgrow(removeArea, Priority.ALWAYS);

		addRow(1, newSectionArea);
		setColumnSpan(newSectionArea, 2);
		setHgrow(newSectionArea, Priority.ALWAYS);
		setVgrow(newSectionArea, Priority.ALWAYS);

		addRow(2, hideArea);
		setColumnSpan(hideArea, 2);
		setHgrow(hideArea, Priority.ALWAYS);
		setVgrow(hideArea, Priority.ALWAYS);
	}

	private Void newSection(final T tag) {
		transHelper.reset();

		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog();
			inputDialog.setTitle("Create new section");
			inputDialog.setHeaderText("");
			inputDialog.setContentText("Section name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return null;
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
				final S section = tagService.addSection(finalAnswer);
				tagService.moveToSection(tag, section);
				return null;
			}
		};
		task.setOnSucceeded(event -> onUpdateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("moving tag {} to new section {}", tag.getName(), finalAnswer, e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(task, "moving tag " + tag.getName() + " to new section " + finalAnswer);

		return null;
	}

	private Void rename(final T tag) {
		transHelper.reset();

		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog(tag.getName());
			inputDialog.setTitle("Rename tag");
			inputDialog.setHeaderText("");
			inputDialog.setContentText("Tag name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return null;
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

		final String finalAnswer = name;
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				tagService.rename(tag, finalAnswer);
				return null;
			}
		};
		task.setOnSucceeded(event -> onUpdateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("renaming tag {} to {}", tag.getName(), finalAnswer, e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(task, "renaming tag " + tag.getName() + " to " + finalAnswer);

		return null;
	}

	private Void remove(final T tag) {
		transHelper.reset();

		final Alert confirmDialog = new Alert(AlertType.CONFIRMATION, "", new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
		confirmDialog.setTitle("Remove label");
		confirmDialog.setHeaderText("Remove label: \"" + tag.getName() + "\"?");
		confirmDialog.setContentText("");
		final Optional<ButtonType> response = confirmDialog.showAndWait();
		if (response.isPresent() && response.get() == ButtonType.OK) {
			final Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					tagService.remove(tag);
					return null;
				}
			};
			task.setOnSucceeded(event -> onUpdateHandler.handle(null));
			task.setOnFailed(e -> LOGGER.error("removing tag {}", tag.getName(), e.getSource().getException()));
			ThreadPool.getInstance().submitHigh(task, "removing tag " + tag.getName());
		}

		return null;
	}

	private Void hide(final T tag) {
		transHelper.reset();

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				tagService.hide(tag);
				return null;
			}
		};
		task.setOnSucceeded(event -> onUpdateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("hiding tag {}", tag.getName(), e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(task, "hiding tag " + tag.getName());

		return null;
	}

	public void setOnUpdate(final EventHandler<ActionEvent> handler) {
		onUpdateHandler = handler;
	}
}
