package net.anfoya.tag.javafx.scene.section;

import java.util.Set;

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
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.IncExcBox;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.javafx.scene.tag.TagList;
import net.anfoya.tag.javafx.scene.tag.TagListItem;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

public class SectionPane<S extends SimpleSection, T extends SimpleTag> extends TitledPane {
//	private static final Logger LOGGER = LoggerFactory.getLogger(SectionPane.class);

	private long sectionTaskId;
	private final TagService<S, T> tagService;
	private final TagList<S, T> tagList;

	private boolean initialized;
	private Labeled titleNode;

	private boolean isTag;
	private TagListItem<SimpleTag> sectionItem;

	private boolean lazyCount;
	private Runnable lazyCountTask;

	private boolean disableWhenZero;
	private DataFormat extItemDataFormat;
	private Timeline expandDelay;

	private EventHandler<ActionEvent> updateHandler;

	@SuppressWarnings("unchecked")
	public SectionPane(final TagService<S, T> tagService, final S section) {
		this.tagService = tagService;
		this.sectionItem = new TagListItem<SimpleTag>(new SimpleTag(section.getId(), section.getName(), section.isSystem()));

		tagList = new TagList<S, T>(tagService, section);
		setContent(tagList);

		isTag = false;
		initialized = false;
		disableWhenZero = false;
		lazyCount = true;
		lazyCountTask = null;

		setOnDragDetected(event -> {
	        final ClipboardContent content = new ClipboardContent();
	        content.put(DndFormat.SECTION_DATA_FORMAT, section);
	        final Dragboard db = startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
		setOnDragEntered(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(extItemDataFormat)) {
				if (!isExpanded()) {
					event.acceptTransferModes(TransferMode.ANY);
					expandDelay = expandAfterDelay();
					event.consume();
				}
			}
		});
		setOnDragOver(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(DndFormat.TAG_DATA_FORMAT)
					&& !section.getId().startsWith(SimpleSection.NO_ID)) { //TODO improve
				final T tag = (T) db.getContent(DndFormat.TAG_DATA_FORMAT);
				if (!tagList.contains(tag)) {
					SectionPane.this.setOpacity(.5);
					event.acceptTransferModes(TransferMode.ANY);
					event.consume();
				}
			} else if (db.hasContent(extItemDataFormat) && !SectionPane.this.isExpanded()) {
				SectionPane.this.setOpacity(.5);
			}
		});
		setOnDragExited(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(extItemDataFormat)) {
				expandDelay = null;
				SectionPane.this.setOpacity(1);
				event.consume();
			} else if (db.hasContent(DndFormat.TAG_DATA_FORMAT)
					&& !section.getId().startsWith(SimpleSection.NO_ID)) { //TODO improve
				SectionPane.this.setOpacity(1);
				event.consume();
			}
		});
		setOnDragDropped(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(DndFormat.TAG_DATA_FORMAT)) {
				final T tag = (T) db.getContent(DndFormat.TAG_DATA_FORMAT);
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
		setOnDragDone(event -> {
			updateHandler.handle(null);
		});

		setExpanded(false);
		expandedProperty().addListener((ov, oldVal, newVal) -> {
			setOpacity(1);
			if (newVal && lazyCountTask != null) {
				Platform.runLater(lazyCountTask);
				lazyCountTask = null;
			}
		});
	}

	private Timeline expandAfterDelay() {
		final Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
		    @Override
		    public void handle(final ActionEvent event) {
		    	if (expandDelay != null) {
		    		setExpanded(true);
		    	}
	    		return;
		    }
		}));
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
					return tagService.getCountForSection(tagList.getSection(), includes, excludes, itemPattern, tagPattern);
				}
			};
			sectionTask.setOnFailed(event -> {
				// TODO Auto-generated catch block
				event.getSource().getException().printStackTrace(System.out);
			});
			sectionTask.setOnSucceeded(event -> {
				if (taskId != sectionTaskId) {
					return;
				}
				sectionItem.countProperty().set(sectionTask.getValue());
			});
			ThreadPool.getInstance().submitHigh(sectionTask);
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

	public void refresh(final Set<T> includes, final Set<T> excludes, final String tagPattern, final String itemPattern) {
		tagList.refresh(includes, excludes, tagPattern, itemPattern);

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
			this.sectionItem = new TagListItem<SimpleTag>(new SimpleTag(sectionItem.getTag().getId(), sectionItem.getTag().getName(), true));
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

	public void setExtItemDataFormat(final DataFormat dataFormat) {
		extItemDataFormat = dataFormat;
		tagList.setExtItemDataFormat(dataFormat);
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
