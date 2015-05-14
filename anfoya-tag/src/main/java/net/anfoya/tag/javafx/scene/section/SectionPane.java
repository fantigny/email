package net.anfoya.tag.javafx.scene.section;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TitledPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
	private final AtomicLong taskId = new AtomicLong();
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

	@SuppressWarnings("unchecked")
	public SectionPane(final TagService<S, T> tagService, final S section, final TagList<S, T> tagList) {
		super("", tagList);
		this.tagService = tagService;
		this.tagList = (TagList<S, T>) getContent();
		this.sectionItem = new TagListItem<SimpleTag>(new SimpleTag(section.getId(), section.getName()));
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
			if (db.hasContent(DndFormat.TAG_DATA_FORMAT)
					&& !section.getId().equals(SimpleSection.NO_ID)) {
				final T tag = (T) db.getContent(DndFormat.TAG_DATA_FORMAT);
				if (!tagList.contains(tag.getName())) {
					SectionPane.this.setOpacity(.5);
					event.consume();
				}
			}
		});
		setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)) {
				SectionPane.this.setOpacity(1);
				event.consume();
			}
		});
		setOnDragOver(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(extItemDataFormat) && !isExpanded()) {
				setExpanded(true);
				event.consume();
			} else if (db.hasContent(DndFormat.TAG_DATA_FORMAT)
					&& !section.getId().startsWith(SimpleSection.NO_ID)) { //TODO improve
				final T tag = (T) db.getContent(DndFormat.TAG_DATA_FORMAT);
				if (!tagList.contains(tag.getName())) {
					event.acceptTransferModes(TransferMode.ANY);
					event.consume();
				}
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

		expandedProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal && lazyCountTask != null) {
				ThreadPool.getInstance().submitLow(lazyCountTask);
				lazyCountTask = null;
			}
			if (!newVal) {
				tagList.getSelectionModel().clearSelection();
			}
		});
	}

	public void updateCountAsync(final int currentCount, final Set<T> availableTags, final Set<T> includes, final Set<T> excludes, final String namePattern, final String tagPattern) {
		if (!isTag) {
			final long taskId = this.taskId.incrementAndGet();
			final Task<Integer> sectionTask = new Task<Integer>() {
				@Override
				protected Integer call() throws TagException, InterruptedException {
					if (Thread.currentThread().isInterrupted()) {
						throw new InterruptedException();
					}
					return tagService.getCountForSection(tagList.getSection(), includes, excludes, namePattern, tagPattern);
				}
			};
			sectionTask.setOnFailed(event -> {
				// TODO Auto-generated catch block
				event.getSource().getException().printStackTrace(System.out);
			});
			sectionTask.setOnSucceeded(event -> {
				if (taskId == this.taskId.get()) {
					sectionItem.countProperty().set(sectionTask.getValue());
				}
			});
			ThreadPool.getInstance().submitHigh(sectionTask);
		} else {
			if (tagList.getSectionItem() == null) {
				sectionItem.countProperty().set(0);
			}
		}

		final Runnable tagListTask = () -> {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			tagList.updateCount(currentCount, availableTags, includes, excludes, namePattern);
		};
		if (!lazyCount || isExpanded()) {
			lazyCountTask = null;
			tagListTask.run();
		} else {
			lazyCountTask = tagListTask;
		}
	}

	public void refresh(final Set<T> includes, final Set<T> excludes, final String tagPattern) {
		tagList.refresh(includes, excludes, tagPattern);

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

	public void setExtItemDataFormat(final DataFormat dataFormat) {
		this.extItemDataFormat = dataFormat;
	}
}
