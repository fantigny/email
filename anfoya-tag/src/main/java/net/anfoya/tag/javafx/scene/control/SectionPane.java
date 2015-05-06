package net.anfoya.tag.javafx.scene.control;

import java.util.Set;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TitledPane;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.IncExcBox;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagService;
import net.anfoya.tag.service.TagServiceException;

public class SectionPane<S extends SimpleSection, T extends SimpleTag> extends TitledPane {
	private final TagService<S, T> tagService;
	private final TagList<S, T> tagList;

	private boolean initialized;
	private Labeled titleNode;

	private boolean isTag;
	private TagListItem<SimpleTag> sectionItem;

	private boolean lazyCount;
	private Runnable lazyCountTask;

	private boolean disableWhenZero;

	@SuppressWarnings("unchecked")
	public SectionPane(final TagService<S, T> tagService, final SimpleSection section, final TagList<S, T> tagList) {
		super("", tagList);
		this.tagService = tagService;
		this.tagList = (TagList<S, T>) getContent();
		this.sectionItem = new TagListItem<SimpleTag>(new SimpleTag(section.getId(), section.getName()));
		isTag = false;
		initialized = false;
		disableWhenZero = false;
		lazyCount = true;
		lazyCountTask = null;

		expandedProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal && lazyCount && lazyCountTask != null) {
				ThreadPool.getInstance().submit(lazyCountTask);
				lazyCountTask = null;
			}
		});
	}

	public void updateCountAsync(final int currentCount, final Set<T> availableTags, final Set<T> includes, final Set<T> excludes, final String namePattern, final String tagPattern) {
		final Runnable tagListTask = new Runnable() {
				@Override
				public void run() {
					tagList.updateCount(currentCount, availableTags, includes, excludes, namePattern);
				}
			};
		if (isExpanded() || !lazyCount) {
			tagListTask.run();
		} else {
			lazyCountTask = tagListTask;
		}
		if (!isTag) {
			final Task<Integer> sectionTask = new Task<Integer>() {
				@Override
				protected Integer call() {
					try {
						return tagService.getCountForSection(tagList.getSection(), includes, excludes, namePattern, tagPattern);
					} catch (final TagServiceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return 0;
					}
				}
			};
			sectionTask.setOnSucceeded(event -> {
				sectionItem.countProperty().set((int) event.getSource().getValue());
			});
			ThreadPool.getInstance().submit(sectionTask);
		} else {
			if (tagList.getSectionItem() == null) {
				sectionItem.countProperty().set(0);
			}
		}
	}

	public void refresh(final Set<T> selectedTags, final String tagPattern) {
		tagList.refresh(selectedTags, tagPattern);

		if (!initialized) {
			init();
		}

		if (isTag) {
			TagListItem<? extends SimpleTag> sectionItem = tagList.getSectionItem();
			if (sectionItem == null) {
				sectionItem = this.sectionItem;
			}

			if (disableWhenZero) {
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
			this.sectionItem = new TagListItem<SimpleTag>(new SimpleTag(sectionItem.getTag().getId(), sectionItem.getTag().getName()));
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
			if (disableWhenZero) {
				disableProperty().bind(sectionItem.disableProperty());
			}
		}

		setGraphic(titleNode);
	}

	public TagList<S, T> getTagList() {
		return tagList;
	}

	public SimpleSection getSection() {
		return tagList.getSection();
	}

	public boolean isDisableWhenZero() {
		return disableWhenZero;
	}

	public void setDisableWhenZero(final boolean disable) {
		this.disableWhenZero = disable;
	}

	public boolean isLazyCount() {
		return lazyCount;
	}

	public void setLazyCount(final boolean lazy) {
		this.lazyCount = lazy;
	}
}
