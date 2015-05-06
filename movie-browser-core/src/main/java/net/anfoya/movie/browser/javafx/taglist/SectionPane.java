package net.anfoya.movie.browser.javafx.taglist;

import java.util.Set;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TitledPane;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.IncExcBox;
import net.anfoya.movie.browser.model.Section;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.movie.browser.service.MovieTagService;

public class SectionPane extends TitledPane {
	private final MovieTagService tagService;
	private final TagList tagList;

	private boolean initialized;
	private Labeled titleNode;

	private boolean isTag;
	private TagListItem sectionItem;

	public SectionPane(final MovieTagService tagService, final Section section, final TagList tagList) {
		super("", tagList);
		this.tagService = tagService;
		this.tagList = (TagList) getContent();
		this.sectionItem = new TagListItem(new Tag(section.getName(), section.getName()));
		this.isTag = false;
		this.initialized = false;
	}

	public void updateMovieCountAsync(final int currentCount, final Set<Tag> availableTags, final Set<Tag> includes, final Set<Tag> excludes, final String namePattern, final String tagPattern) {
		tagList.updateMovieCount(currentCount, availableTags, includes, excludes, namePattern);
		if (!isTag) {
			final Task<Integer> task = new Task<Integer>() {
				@Override
				protected Integer call() {
					return tagService.getCountForSection(tagList.getSection(), includes, excludes, namePattern, tagPattern);
				}
			};
			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(final WorkerStateEvent event) {
					sectionItem.countProperty().set((int) event.getSource().getValue());
				}
			});
			ThreadPool.getInstance().submit(task);
		} else {
			if (tagList.getSectionItem() == null) {
				sectionItem.countProperty().set(0);
			}
		}
	}

	public void refresh(final Set<Tag> selectedTags, final String tagPattern) {
		tagList.refresh(selectedTags, tagPattern);

		if (!initialized) {
			init();
		}

		if (isTag) {
			TagListItem sectionItem = tagList.getSectionItem();
			if (sectionItem == null) {
				sectionItem = this.sectionItem;
			}

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
