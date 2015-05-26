package net.anfoya.tag.javafx.scene.section;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.ResetTextField;
import net.anfoya.javafx.scene.control.Title;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.javafx.scene.dnd.ExtItemDropPane;
import net.anfoya.tag.javafx.scene.tag.TagDropPane;
import net.anfoya.tag.javafx.scene.tag.TagList;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

public class SectionListPane<S extends SimpleSection, T extends SimpleTag> extends BorderPane {
	private final TagService<S, T> tagService;
	private final DataFormat extItemDataFormat;

	private final ResetTextField tagPatternField;
	private final Accordion sectionAcc;
	private final SelectedTagsPane<T> selectedTagsPane;

	private Set<S> sections;
	private EventHandler<ActionEvent> selectTagHandler;
	private EventHandler<ActionEvent> updateSectionHandler;

	private boolean sectionDisableWhenZero;

	private boolean lazyCount;
	private String itemPattern;

	private String tagPattern;

	private Task<Set<S>> refreshTask;
	private int refreshTaskId;

	private Timeline tagPatternDelay;
	private SectionDropPane<S> sectionDropPane;
	private TagDropPane<S, T> tagDropPane;

	public SectionListPane(final TagService<S, T> tagService) {
		this(tagService, null);
	}

	public SectionListPane(final TagService<S, T> tagService, final DataFormat extItemDataFormat) {
		this.tagService = tagService;
		this.extItemDataFormat = extItemDataFormat;

		lazyCount = true;
		sectionDisableWhenZero = true;

		tagPatternField = new ResetTextField();
		tagPatternField.prefWidthProperty().bind(widthProperty());
		tagPatternField.setPromptText("search");
		tagPatternField.textProperty().addListener((ChangeListener<String>) (ov, oldPattern, newPattern) -> {
			refreshWithTagPattern();
		});

		final HBox patternBox = new HBox(5, new Title("Label"), tagPatternField);
		setTop(patternBox);

		sectionAcc = new Accordion();
		sectionAcc.expandedPaneProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal != null && newVal.isExpanded() && !isCheckMode()) {
				@SuppressWarnings("unchecked")
				final TagList<S, T> tagList = (TagList<S, T>) newVal.getContent();
				if (tagList.getSelectedTag() != null) {
					selectTagHandler.handle(null);
				}
			}
		});

		final StackPane stackPane = new StackPane(sectionAcc);
		stackPane.setAlignment(Pos.BOTTOM_CENTER);
		stackPane.setPadding(new Insets(5, 0, 5, 0));

		final ExtItemDropPane<T> extItemDropPane = new ExtItemDropPane<T>(extItemDataFormat);
		extItemDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		sectionDropPane = new SectionDropPane<S>(tagService);
		sectionDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		tagDropPane = new TagDropPane<S, T>(tagService);
		tagDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		stackPane.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(DndFormat.SECTION_DATA_FORMAT)
					&& !stackPane.getChildren().contains(sectionDropPane)) {
				stackPane.getChildren().add(sectionDropPane);
			} else if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)
					&& !stackPane.getChildren().contains(tagDropPane)) {
				stackPane.getChildren().add(tagDropPane);
			} else if (extItemDataFormat != null
					&& event.getDragboard().hasContent(extItemDataFormat)
					&& !stackPane.getChildren().contains(extItemDropPane)) {
				stackPane.getChildren().add(extItemDropPane);
			}
		});
		stackPane.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DndFormat.SECTION_DATA_FORMAT)
					&& stackPane.getChildren().contains(sectionDropPane)) {
				stackPane.getChildren().remove(sectionDropPane);
			} else if (event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)
					&& stackPane.getChildren().contains(tagDropPane)) {
				stackPane.getChildren().remove(tagDropPane);
			} else if (extItemDataFormat != null
					&& event.getDragboard().hasContent(extItemDataFormat)
					&& stackPane.getChildren().contains(extItemDropPane)) {
				stackPane.getChildren().remove(extItemDropPane);
			}
		});
		setCenter(stackPane);

		selectedTagsPane = new SelectedTagsPane<T>();
		selectedTagsPane.setClearTagCallBack(tag -> {
			clear(tag.getName());
			return null;
		});
		setBottom(selectedTagsPane);
	}

	private void refreshSections() {
		// delete sections
		final Set<SimpleSection> existingSections = new LinkedHashSet<SimpleSection>();
		for(final Iterator<TitledPane> i = sectionAcc.getPanes().iterator(); i.hasNext();) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) i.next().getContent();
			if (sections.contains(tagList.getSection())) {
				existingSections.add(tagList.getSection());
			} else {
				i.remove();
			}
		}

		// add new sections
		int index = 0;
		for(final S section: sections) {
			if (!existingSections.contains(section)) {
				final SectionPane<S, T> sectionPane = new SectionPane<S, T>(tagService, section);
				sectionPane.setDisableWhenZero(sectionDisableWhenZero);
				sectionPane.setLazyCount(lazyCount);
				sectionPane.setOnSelectTag(selectTagHandler);
				sectionPane.setOnUpdateSection(updateSectionHandler);
				sectionPane.setExtItemDataFormat(extItemDataFormat);
				sectionAcc.getPanes().add(index, sectionPane);
			}
			index++;
		}
	}

	private void refreshTags() {
		final String tagPattern = tagPatternField.getText();
		final Set<T> includes = getIncludedTags();
		final Set<T> excludes = getExcludedTags();
		for(final TitledPane pane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final SectionPane<S, T> sectionPane = (SectionPane<S, T>) pane;
			sectionPane.refresh(includes, excludes, itemPattern, tagPattern);
		}
	}

	public void refreshAsync() {
		refreshAsync(null);
	}

	public synchronized void refreshAsync(final Callback<Void, Void> callback) {
		final long taskId = ++refreshTaskId;
		if (refreshTask != null && refreshTask.isRunning()) {
			refreshTask.cancel();
		}

		refreshTask = new Task<Set<S>>() {
			@Override
			protected Set<S> call() throws InterruptedException, TagException {
				return tagService.getSections();
			}
		};
		refreshTask.setOnSucceeded(event -> {
			if (taskId != refreshTaskId) {
				return;
			}

			sections = refreshTask.getValue();
			refreshSections();
			refreshTags();
			selectedTagsPane.refresh(getAllSelectedTags());
			if (callback != null) {
				callback.call(null);
			}
		});
		refreshTask.setOnFailed(event -> {
			// TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out);
		});
		ThreadPool.getInstance().submitHigh(refreshTask);
	}

	private synchronized void refreshWithTagPattern() {
		if (tagPatternDelay != null) {
			tagPatternDelay.stop();
		}

		tagPatternDelay = new Timeline(new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
		    @Override
		    public void handle(final ActionEvent event) {
	    		refreshAsync();
		    }
		}));
		tagPatternDelay.setCycleCount(1);
		tagPatternDelay.play();
	}

	public void clear(final String tagName) {
		for(final TitledPane sectionPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) sectionPane.getContent();
			tagList.clear(tagName);
		}
	}

	public void init(final String sectionName, final String tagName) {
		refreshAsync(v -> {
			for(final TitledPane sectionPane: sectionAcc.getPanes()) {
				@SuppressWarnings("unchecked")
				final TagList<S, T> tagList = (TagList<S, T>) sectionPane.getContent();
				if (tagList.getSection().getName().equals(sectionName)) {
					sectionPane.setExpanded(true);
					tagList.selectLight(tagName);
					break;
				}
			}
			return null;
		});
	}

	public void setOnSelectTag(final EventHandler<ActionEvent> handler) {
		selectTagHandler = event -> {
			selectedTagsPane.refresh(getAllSelectedTags());
			handler.handle(event);
		};
	}

	public void setOnUpdateSection(final EventHandler<ActionEvent> handler) {
		updateSectionHandler = handler;
		sectionDropPane.setOnUpdateSection(handler);
	}

	public void updateItemCount(final Set<T> toRefresh, final String itemPattern, final boolean lazy) {
		updateItemCount(toRefresh, -1, itemPattern, lazy);
	}

	public void updateItemCount(Set<T> toRefresh, final int queryCount, final String itemPattern, final boolean lazy) {
		this.itemPattern = itemPattern;
		tagPattern = tagPatternField.getText();
		final Set<T> includes = getIncludedTags();
		final Set<T> excludes = getExcludedTags();
		final boolean checkMode = isCheckMode();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final SectionPane<S, T> sectionPane = (SectionPane<S, T>) titledPane;
			if (!checkMode) {
				try {
					toRefresh = tagService.getTags(sectionPane.getSection(), itemPattern);
				} catch (final TagException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sectionPane.updateCountAsync(queryCount, toRefresh, includes, excludes, itemPattern, tagPattern);
		}
	}

	public Set<T> getAllTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) titledPane.getContent();
			tags.addAll(tagList.getTags());
		}

		return Collections.unmodifiableSet(tags);
	}

	public Set<T> getAllSelectedTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		tags.addAll(getIncludedTags());
		tags.addAll(getExcludedTags());
		return tags;
	}

	public Set<T> getIncludedOrSelectedTags() {
		Set<T> included;
		if (isCheckMode()) {
			included = getIncludedTags();
		} else {
			included = new HashSet<T>();
			final T tag = getSelectedTag();
			if (tag != null) {
				included.add(tag);
			}
		}
		return included;
	}

	private Set<T> getIncludedTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) titledPane.getContent();
			tags.addAll(tagList.getIncludedTags());
		}

		return Collections.unmodifiableSet(tags);
	}

	public Set<T> getExcludedTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) titledPane.getContent();
			tags.addAll(tagList.getExcludedTags());
		}
		return Collections.unmodifiableSet(tags);
	}

	public boolean isSectionDisableWhenZero() {
		return sectionDisableWhenZero;
	}

	public void setSectionDisableWhenZero(final boolean disable) {
		this.sectionDisableWhenZero = disable;
	}

	public boolean isLazyCount() {
		return lazyCount;
	}

	public void setLazyCount(final boolean lazy) {
		this.lazyCount = lazy;
	}

	public S getSectionAt(final double x, final double y) {
		return null;
	}

	private T getSelectedTag() {
		T tag = null;
		for(final TitledPane sectionPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) sectionPane.getContent();
			if (sectionPane.isExpanded()) {
				tag = tagList.getSelectedTag();
				if (tag != null) {
					break;
				}
			}
		}

		return tag;
	}

	private boolean isCheckMode() {
		boolean checkMode = false;
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) titledPane.getContent();
			checkMode = tagList.hasCheckedTag();
			if (checkMode) {
				break;
			}
		}
		return checkMode;
	}

	public void setOnUpdateTag(final EventHandler<ActionEvent> handler) {
		tagDropPane.setOnUpdate(handler);
	}
}
