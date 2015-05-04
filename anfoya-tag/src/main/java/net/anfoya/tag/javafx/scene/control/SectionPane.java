package net.anfoya.tag.javafx.scene.control;

import java.util.Set;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TitledPane;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.IncExcBox;
import net.anfoya.tag.model.Section;
import net.anfoya.tag.model.Tag;
import net.anfoya.tag.service.TagService;
import net.anfoya.tag.service.TagServiceException;

public class SectionPane extends TitledPane {
	private final TagService tagService;
	private final TagList tagList;

	private boolean initialized;
	private Labeled titleNode;

	private boolean isTag;
	private TagListItem sectionItem;

	private boolean isDisableWhenZero;

	public SectionPane(final TagService tagService, final Section section, final TagList tagList) {
		super("", tagList);
		this.tagService = tagService;
		this.tagList = (TagList) getContent();
		sectionItem = new TagListItem(new Tag(section.getId(), section.getName()));
		isTag = false;
		initialized = false;
		isDisableWhenZero = true;
	}

	public void updateCountAsync(final int currentCount, final Set<Tag> availableTags, final Set<Tag> includes, final Set<Tag> excludes, final String namePattern, final String tagPattern) {
		tagList.updateCount(currentCount, availableTags, includes, excludes, namePattern);
		if (!isTag) {
			final Task<Integer> task = new Task<Integer>() {
				@Override
				protected Integer call() {
					try {
						return tagService.getSectionCount(tagList.getSection(), includes, excludes, namePattern, tagPattern);
					} catch (final TagServiceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return 0;
					}
				}
			};
			task.setOnSucceeded(event -> sectionItem.countProperty().set((int) event.getSource().getValue()));
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

			if (isDisableWhenZero) {
				disableProperty().unbind();
				disableProperty().bind(sectionItem.disableProperty());
			}

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
		isTag = tagList.isStandAloneSectionTag();

		if (isTag) {
			titleNode = new IncExcBox();
			setExpanded(false);
			setCollapsible(false);
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

	public boolean isDisableWhenZero() {
		return isDisableWhenZero;
	}

	public void setDisableWhenZero(final boolean disable) {
		this.isDisableWhenZero = disable;
	}
}
