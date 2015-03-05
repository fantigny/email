package net.anfoya.movies.javafx.taglist;

import java.util.Set;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TitledPane;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.IncExcBox;
import net.anfoya.movies.model.Section;
import net.anfoya.movies.model.Tag;
import net.anfoya.movies.service.TagService;

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

	public void updateMovieCountAsync(final int currentCount, final Set<Tag> availableTags, final Set<Tag> tags, final Set<Tag> excludes, final String namePattern) {
		tagList.updateMovieCount(currentCount, availableTags, tags, excludes, namePattern);
		if (!isTag) {
			final Task<Integer> task = new Task<Integer>() {
				@Override
				protected Integer call() {
					return tagService.getSectionMovieCount(tagList.getSection(), tags, excludes, namePattern);
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

			final IncExcBox incExcBox = (IncExcBox) titleNode;
			incExcBox.textProperty().unbind();
			incExcBox.textProperty().bind(sectionItem.textProperty());
			if (this.sectionItem != null) {
				incExcBox.includedProperty().unbindBidirectional(this.sectionItem.includedProperty());
				incExcBox.excludedProperty().unbindBidirectional(this.sectionItem.excludedProperty());
			}
			incExcBox.includedProperty().bindBidirectional(sectionItem.includedProperty());
			incExcBox.excludedProperty().bindBidirectional(sectionItem.excludedProperty());
			this.sectionItem = sectionItem;
		}
	}

	public void init() {
		initialized = true;
		isTag = tagList.getSectionItem() != null;

		if (isTag) {
			titleNode = new IncExcBox();
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
