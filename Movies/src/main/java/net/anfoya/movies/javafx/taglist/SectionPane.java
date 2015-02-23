package net.anfoya.movies.javafx.taglist;

import java.util.Set;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.movies.model.Section;
import net.anfoya.movies.model.Tag;
import net.anfoya.movies.service.TagService;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TitledPane;

public class SectionPane extends TitledPane {
	private final TagService tagService;
	private final TagList tagList;

	private boolean initialized;
	private Labeled titleNode;

	private boolean isTag;
	private TagListItem sectionItem;

	public SectionPane(final TagService tagService, final Section section, final TagList tagList) {
		super("", tagList);
		this.tagService = tagService;
		this.tagList = (TagList) getContent();
		this.sectionItem = new TagListItem(new Tag(section.getName(), section.getName()));
		this.isTag = false;
		this.initialized = false;
	}

	public void updateMovieCount(final int currentCount, final Set<Tag> availableTags, final Set<Tag> selectedTags, final String namePattern) {
		tagList.updateMovieCount(currentCount, availableTags, selectedTags, namePattern);
		if (!isTag) {
			final Task<Integer> task = new Task<Integer>() {
				@Override
				protected Integer call() {
					return tagService.getSectionMovieCount(tagList.getSection(), selectedTags, namePattern);
				}
			};
			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(final WorkerStateEvent event) {
					sectionItem.movieCountProperty().set((int) event.getSource().getValue());
				}
			});
			ThreadPool.getInstance().submit(task);
		}
	}

	public void refresh(final Set<Tag> selectedTags) {
		tagList.refresh(selectedTags);

		if (!initialized) {
			init();
		}

		if (isTag) {
			final TagListItem sectionItem = tagList.getSectionItem();

			disableProperty().unbind();
			disableProperty().bind(sectionItem.disableProperty());

			final CheckBox checkBox = (CheckBox) titleNode;
			checkBox.textProperty().unbind();
			checkBox.textProperty().bind(sectionItem.textProperty());
			if (this.sectionItem != null) {
				checkBox.selectedProperty().unbindBidirectional(this.sectionItem.selectedProperty());
			}
			checkBox.selectedProperty().bindBidirectional(sectionItem.selectedProperty());
			this.sectionItem = sectionItem;
		}
	}

	public void init() {
		initialized = true;
		isTag = tagList.getSectionItem() != null;

		if (isTag) {
			titleNode = new CheckBox();
			if (tagList.isStandAloneSectionTag()) {
				setExpanded(false);
				setCollapsible(false);
			}
		} else {
			titleNode = new Label();
			titleNode.textProperty().bind(sectionItem.textProperty());
			disableProperty().bind(sectionItem.disableProperty());
		}

		setGraphic(titleNode);
	}

	public TagList getTagList() {
		return tagList;
	}

	public Section getSection() {
		return tagList.getSection();
	}
}
