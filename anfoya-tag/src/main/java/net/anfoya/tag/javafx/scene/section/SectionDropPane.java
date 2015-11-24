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
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.tag.service.Section;
import net.anfoya.tag.service.Tag;
import net.anfoya.tag.service.TagService;

public class SectionDropPane<S extends Section> extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(SectionDropPane.class);

	private final TagService<S, ? extends Tag> tagService;

	private EventHandler<ActionEvent> updateHandler;

	public SectionDropPane(final TagService<S, ? extends Tag> tagService) {
		this.tagService = tagService;

		getStyleClass().add("droparea-grid");

		final DropArea removeArea = new DropArea("remove", Section.SECTION_DATA_FORMAT);
		removeArea.<S>setDropCallback((S s) -> remove(s));

		final DropArea renameArea = new DropArea("rename", Section.SECTION_DATA_FORMAT);
		renameArea.<S>setDropCallback((S s) -> rename(s));

		final DropArea hideArea = new DropArea("hide", Section.SECTION_DATA_FORMAT);
		hideArea.<S>setDropCallback((S s) -> hide(s));

		int i = 0;
		addRow(i++, renameArea);
		setColumnSpan(renameArea, 2);
		setHgrow(renameArea, Priority.ALWAYS);
		setVgrow(renameArea, Priority.ALWAYS);

		addRow(i++, hideArea, removeArea);
		setHgrow(hideArea, Priority.ALWAYS);
		setVgrow(hideArea, Priority.ALWAYS);
		setHgrow(removeArea, Priority.ALWAYS);
		setVgrow(removeArea, Priority.ALWAYS);

		setMaxHeight(65 * i);
	}

	public void setOnUpdateSection(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	private Void rename(final S section) {
		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog(section.getName());
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
				tagService.rename(section, finalAnswer);
				return null;
			}
		};
		task.setOnSucceeded(e -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("renaming section {} to {}", section.getName(), finalAnswer, e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(task, "renaming section " + section.getName() + " to " + finalAnswer);

		return null;
	}

	private Void remove(final S section) {
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
			task.setOnSucceeded(event -> updateHandler.handle(null));
			task.setOnFailed(e -> LOGGER.error("removing section {}", section.getName(), e.getSource().getException()));
			ThreadPool.getInstance().submitHigh(task, "removing section " + section.getName());
		}

		return null;
	}

	private Void hide(final S section) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				tagService.hide(section);
				return null;
			}
		};
		task.setOnSucceeded(event -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("hiding section {}", section.getName(), e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(task, "hiding section " + section.getName());

		return null;
	}
}
