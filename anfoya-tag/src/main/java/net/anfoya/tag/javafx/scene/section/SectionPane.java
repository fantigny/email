package net.anfoya.tag.javafx.scene.section;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TitledPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.IncExcBox;
import net.anfoya.tag.javafx.scene.dnd.ExtItemDropPane;
import net.anfoya.tag.javafx.scene.tag.TagList;
import net.anfoya.tag.javafx.scene.tag.TagListItem;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.Section;
import net.anfoya.tag.service.Tag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

public class SectionPane<S extends Section, T extends Tag> extends TitledPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(SectionPane.class);

	private long sectionTaskId;
	private final TagService<S, T> tagService;
	private final TagList<S, T> tagList;

	private boolean initialized;
	private Labeled titleNode;

	private boolean isTag;
	private TagListItem<Tag> sectionItem;

	private boolean lazyCount;
	private Runnable lazyCountTask;

	private boolean disableWhenZero;
	private Timeline expandDelay;

	private EventHandler<ActionEvent> updateHandler;

	@SuppressWarnings("unchecked")
	public SectionPane(final TagService<S, T> tagService, final S section, final boolean showExcludeBox) {
		this.tagService = tagService;
		if (section == null) {
			this.sectionItem = null;
		} else {
			this.sectionItem = new TagListItem<Tag>(new SimpleTag(section.getId(), section.getName(), section.getName(), section.isSystem()));
		}

		tagList = new TagList<S, T>(tagService, section, showExcludeBox);
		tagList.focusTraversableProperty().bind(focusTraversableProperty());
		setContent(tagList);

		isTag = false;
		initialized = false;
		disableWhenZero = false;
		lazyCount = true;
		lazyCountTask = null;

		setOnDragDetected(event -> {
			if (section != null) {
		        final ClipboardContent content = new ClipboardContent();
		        content.put(Section.SECTION_DATA_FORMAT, section);
		        final Dragboard db = startDragAndDrop(TransferMode.ANY);
		        db.setContent(content);
			}
		});
		setOnDragEntered(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT)) {
				if (!isExpanded()) {
					event.acceptTransferModes(TransferMode.ANY);
					expandDelay = expandAfterDelay();
					event.consume();
				}
			}
		});
		setOnDragOver(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(Tag.TAG_DATA_FORMAT)
					&& section != null
					&& !section.getId().startsWith(Section.NO_ID)) { //TODO improve
				final T tag = (T) db.getContent(Tag.TAG_DATA_FORMAT);
				if (!tagList.contains(tag)) {
					SectionPane.this.setOpacity(.5);
					event.acceptTransferModes(TransferMode.ANY);
					event.consume();
				}
			} else if (db.hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT) && !SectionPane.this.isExpanded()) {
				SectionPane.this.setOpacity(.5);
			}
		});
		setOnDragExited(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT)) {
				expandDelay = null;
				SectionPane.this.setOpacity(1);
				event.consume();
			} else if (db.hasContent(Tag.TAG_DATA_FORMAT)
					&& section != null
					&& !section.getId().startsWith(Section.NO_ID)) { //TODO improve
				SectionPane.this.setOpacity(1);
				event.consume();
			}
		});
		setOnDragDropped(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(Tag.TAG_DATA_FORMAT)
					&& section != null) {
				final T tag = (T) db.getContent(Tag.TAG_DATA_FORMAT);
				try {
					tagService.moveToSection(tag, section);
					event.setDropCompleted(true);
					event.consume();
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		setOnDragDone(e -> {
			updateHandler.handle(null);
		});

		setExpanded(section == null);
		expandedProperty().addListener((ov, o, n) -> {
			setOpacity(1);
			if (n && lazyCountTask != null) {
				Platform.runLater(lazyCountTask);
				lazyCountTask = null;
			}
		});
	}

	private Timeline expandAfterDelay() {
		final Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> setExpanded(isExpanded() || expandDelay != null)));
		timeline.setCycleCount(1);
		timeline.play();

		return timeline;
	}

	public synchronized void updateCountAsync(final int queryCount, final Set<T> tags, final Set<T> includes, final Set<T> excludes, final String itemPattern, final String tagPattern) {
		if (!isTag) {
			final long taskId = ++sectionTaskId;
			final Task<Integer> sectionTask = new Task<Integer>() {
				@Override
				protected Integer call() throws TagException {
					return tagService.getCountForSection(tagList.getSection(), includes, excludes, itemPattern);
				}
			};
			sectionTask.setOnSucceeded(event -> {
				if (taskId != sectionTaskId) {
					return;
				}
				sectionItem.countProperty().set(sectionTask.getValue());
			});
			sectionTask.setOnFailed(e -> LOGGER.error("getting message count for section {}", tagList.getSection().getName(), e.getSource().getException()));
			ThreadPool.getInstance().submitLow(sectionTask, "getting message count for section " + tagList.getSection().getName());
		} else {
			if (tagList.getSectionItem() == null) {
				sectionItem.countProperty().set(0);
			}
		}

		final Runnable tagListTask = () -> {
			tagList.updateCount(queryCount, tags, includes, excludes, itemPattern);
		};
		if (!lazyCount || isExpanded()) {
			lazyCountTask = null;
			tagListTask.run();
		} else {
			lazyCountTask = tagListTask;
		}
	}

	public void refresh(final String pattern, final Set<T> includes, final Set<T> excludes, final String itemPattern) {
		tagList.refresh(pattern, includes, excludes, itemPattern);

		if (!initialized) {
			init();
		}

		if (isTag) {
			TagListItem<? extends Tag> sectionItem = tagList.getSectionItem();
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
			final Tag tag = sectionItem.getTag();
			this.sectionItem = new TagListItem<Tag>(new SimpleTag(tag.getId(), tag.getName(), tag.getName(), true));
		}
	}

	protected void init() {
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

	public S getSection() {
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

	public void setOnSelectTag(final EventHandler<ActionEvent> handler) {
		tagList.setOnIncExcTag(handler);
		tagList.setOnSelectTag(event -> {
			if (isExpanded()) {
				handler.handle(event);
			}
		});
	}

	public void setOnUpdateSection(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	public void clearSelection() {
		tagList.getSelectionModel().clearSelection();
	}
}
