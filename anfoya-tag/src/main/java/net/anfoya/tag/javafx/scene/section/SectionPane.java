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
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.javafx.scene.control.IncExcBox;
import net.anfoya.javafx.scene.dnd.DndHelper;
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

	private final TagService<S, T> tagService;
	private final TagList<S, T> tagList;

	private Labeled titleNode;

	private long sectionTaskId;
	private boolean initialized;

	private boolean isTag;
	private TagListItem<Tag> sectionItem;

	private boolean lazyCount;
	private Runnable lazyCountTask;

	private boolean disableWhenZero;

	private boolean delayedExpand;

	private EventHandler<ActionEvent> updateHandler;

	public SectionPane(final S section, final TagService<S, T> tagService, final boolean showExcludeBox) {
		getStyleClass().add("section-pane");
		this.tagService = tagService;

		if (section == null) {
			this.sectionItem = null;
		} else {
			this.sectionItem = new TagListItem<Tag>(new SimpleTag(section.getId(), section.getName(), section.getName(), section.isSystem()));
		}

		tagList = new TagList<S, T>(section, tagService, showExcludeBox);
		tagList.focusTraversableProperty().bind(focusTraversableProperty());
		setContent(tagList);

		isTag = false;
		initialized = false;
		disableWhenZero = false;
		lazyCount = true;
		lazyCountTask = null;

		setOnDragDetected(e -> {
			if (section != null && !section.isSystem()) {
		        final ClipboardContent content = new ClipboardContent();
		        content.put(Section.SECTION_DATA_FORMAT, section);

		        final Image image = new DndHelper(getScene().getStylesheets()).textToImage(section.getName());

				final Dragboard db = startDragAndDrop(TransferMode.ANY);
				db.setContent(content);
				db.setDragView(image, image.getWidth() / 2, image.getHeight() / 2);
				e.consume();
			}
		});
		setOnDragEntered(e -> {
			final Dragboard db = e.getDragboard();
			if (section != null
					&& !section.getId().startsWith(Section.NO_ID) //TODO improve
					&& db.hasContent(Tag.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) db.getContent(Tag.TAG_DATA_FORMAT);
				if (!tagList.contains(tag)) {
					Platform.runLater(() -> getStyleClass().add("section-pane-dnd-highlight"));
				}
			} else if (db.hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT) && !isExpanded()) {
				Platform.runLater(() -> getStyleClass().add("section-pane-dnd-highlight"));
				expandAfterDelay();
			}
			e.consume();
		});
		setOnDragOver(e -> {
			final Dragboard db = e.getDragboard();
			if (section != null
					&& !section.getId().startsWith(Section.NO_ID) //TODO improve
					&& db.hasContent(Tag.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) db.getContent(Tag.TAG_DATA_FORMAT);
				if (!tagList.contains(tag)) {
					e.acceptTransferModes(TransferMode.LINK);
				}
			}
			e.consume();
		});
		setOnDragExited(e -> {
			Platform.runLater(() -> getStyleClass().remove("section-pane-dnd-highlight"));
			delayedExpand = false;
			e.consume();
		});
		setOnDragDropped(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasContent(Tag.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) db.getContent(Tag.TAG_DATA_FORMAT);
				move(section, tag);
			}
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

	private void move(S section, T tag) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws TagException {
				tagService.moveToSection(tag, section);
				return null;
			}
		};
		task.setOnSucceeded(e -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("move {} to {}", tag, section, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "move " + tag.getName() + " to " + section.getName(), task);
	}

	private void expandAfterDelay() {
		final KeyFrame keyFrame = new KeyFrame(Duration.millis(500), e -> {
			Platform.runLater(() -> getStyleClass().remove("section-pane-dnd-highlight"));
			if (!isExpanded() && delayedExpand) {
				setExpanded(true);
			}
		});

		final Timeline timeline = new Timeline(keyFrame);
		timeline.setCycleCount(1);
		timeline.play();

		delayedExpand = true;
	}

	public synchronized void updateCountAsync(final int queryCount, final Set<T> tags, final Set<T> includes, final Set<T> excludes, final String itemPattern, final String tagPattern) {
		if (!isTag) {
			final long taskId = ++sectionTaskId;
			final Task<Long> sectionTask = new Task<Long>() {
				@Override
				protected Long call() throws TagException {
					return tagService.getCountForSection(tagList.getSection(), includes, excludes, itemPattern);
				}
			};
			sectionTask.setOnSucceeded(event -> {
				if (taskId != sectionTaskId) {
					return;
				}
				sectionItem.countProperty().set(sectionTask.getValue());
			});
			sectionTask.setOnFailed(e -> LOGGER.error("get message count for section {}", tagList.getSection().getName(), e.getSource().getException()));
			ThreadPool.getDefault().submit(PoolPriority.MIN, "get message count for section " + tagList.getSection().getName(), sectionTask);
		} else {
			if (tagList.getSectionItem() == null) {
				sectionItem.countProperty().set(0);
			}
		}

		final Runnable tagListTask = () -> tagList.updateCount(queryCount, tags, includes, excludes, itemPattern);
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

		titleNode.getStyleClass().add("section-pane-title");
		if (sectionItem.getTag().isSystem()) {
			titleNode.getStyleClass().add("system");
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
		tagList.setOnSelectTag(e -> {
			if (isExpanded()) {
				handler.handle(e);
			}
		});
	}

	public void setOnUpdateSection(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	public void clearSelection() {
		tagList.getSelectionModel().clearSelection();
	}

	public void selectLight(String tagName) {
		if (!isTag) {
			setExpanded(true);
		}
		tagList.selectLight(tagName);
	}
}
