package net.anfoya.tag.javafx.scene.section;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import net.anfoya.javafx.scene.animation.DelayTimeline;
import net.anfoya.javafx.scene.control.ResetTextField;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.javafx.scene.dnd.ExtItemDropPane;
import net.anfoya.tag.javafx.scene.tag.TagDropPane;
import net.anfoya.tag.javafx.scene.tag.TagList;
import net.anfoya.tag.service.Section;
import net.anfoya.tag.service.Tag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionListPane<S extends Section, T extends Tag> extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(SectionListPane.class);

	private final TagService<S, T> tagService;
	private final DataFormat extItemDataFormat;

	private final ResetTextField patternField;
	private final Accordion sectionAcc;
	private final SelectedTagsPane<T> selectedTagsPane;

	private Set<S> sections;
	private EventHandler<ActionEvent> selectTagHandler;
	private EventHandler<ActionEvent> updateSectionHandler;

	private boolean sectionDisableWhenZero;

	private boolean lazyCount;
	private String itemPattern;

	private Task<Void> refreshTask;
	private int refreshTaskId;

	private DelayTimeline patternDelay;
	private SectionDropPane<S> sectionDropPane;
	private TagDropPane<S, T> tagDropPane;

	private boolean showExcludeBox;

	private String initSectionName;
	private String initTagName;

	public SectionListPane(final TagService<S, T> tagService) {
		this(tagService, null, false);
	}

	public SectionListPane(final TagService<S, T> tagService, final DataFormat extItemDataFormat, final boolean withExcludeBox) {
		this.tagService = tagService;
		this.extItemDataFormat = extItemDataFormat;
		this.showExcludeBox = withExcludeBox;

		lazyCount = true;
		sectionDisableWhenZero = true;

		patternField = new ResetTextField();
		patternField.prefWidthProperty().bind(widthProperty());
		patternField.setPromptText("label search");
		patternField.textProperty().addListener((ov, o, n) -> refreshWithPattern());
		setTop(new HBox(5, patternField));

		sectionAcc = new Accordion();
		sectionAcc.expandedPaneProperty().addListener((ov, o, n) -> {
			if (n == null && !sectionAcc.getPanes().isEmpty()) {
				new Timer("section-auto-expand-timer", true).schedule(new TimerTask() {
					@Override
					public void run() {
						if (sectionAcc.expandedPaneProperty().isNull().get()
								 && !sectionAcc.getPanes().isEmpty()) {
							sectionAcc.setExpandedPane(sectionAcc.getPanes().get(0));
						}
					}
				}, 500);
			}
		});

		final StackPane stackPane = new StackPane(sectionAcc);
		stackPane.setAlignment(Pos.BOTTOM_CENTER);

		final ExtItemDropPane<T> extItemDropPane = new ExtItemDropPane<T>(extItemDataFormat);
		extItemDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		sectionDropPane = new SectionDropPane<S>(tagService);
		sectionDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		tagDropPane = new TagDropPane<S, T>(tagService);
		tagDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		stackPane.setOnDragEntered(e -> {
			if (e.getDragboard().hasContent(DndFormat.SECTION_DATA_FORMAT)
					&& !stackPane.getChildren().contains(sectionDropPane)) {
				stackPane.getChildren().add(sectionDropPane);
			} else if (e.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)
					&& !stackPane.getChildren().contains(tagDropPane)) {
				stackPane.getChildren().add(tagDropPane);
			} else if (extItemDataFormat != null
					&& e.getDragboard().hasContent(extItemDataFormat)
					&& !stackPane.getChildren().contains(extItemDropPane)) {
				stackPane.getChildren().add(extItemDropPane);
			}
		});
		stackPane.setOnDragExited(e -> {
			if (e.getDragboard().hasContent(DndFormat.SECTION_DATA_FORMAT)
					&& stackPane.getChildren().contains(sectionDropPane)) {
				stackPane.getChildren().remove(sectionDropPane);
			} else if (e.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT)
					&& stackPane.getChildren().contains(tagDropPane)) {
				stackPane.getChildren().remove(tagDropPane);
			} else if (extItemDataFormat != null
					&& e.getDragboard().hasContent(extItemDataFormat)
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
		final Set<Section> existingSections = new LinkedHashSet<Section>();
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
				final SectionPane<S, T> sectionPane = new SectionPane<S, T>(tagService, section, showExcludeBox);
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
		final String pattern = patternField.getText();
		final Set<T> includes = getIncludedTags();
		final Set<T> excludes = getExcludedTags();
		for(final TitledPane pane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final SectionPane<S, T> sectionPane = (SectionPane<S, T>) pane;
			sectionPane.refresh(pattern, includes, excludes, itemPattern);
		}
	}

	public synchronized void refreshAsync(final Callback<Void, Void> callback) {
		final long taskId = ++refreshTaskId;
		if (refreshTask != null && refreshTask.isRunning()) {
			refreshTask.cancel();
		}

		if (!patternField.getText().isEmpty()) {
			refreshWithPatternAsync();
		}

		refreshTask = new Task<Void>() {
			@Override
			protected Void call() throws InterruptedException, TagException {
				sections = tagService.getSections();
				return null;
			}
		};
		refreshTask.setOnSucceeded(e -> {
			if (taskId != refreshTaskId) {
				return;
			}

			refreshSections();
			refreshTags();
			selectedTagsPane.refresh(getAllSelectedTags());
			if (callback != null) {
				callback.call(null);
			}
		});
		refreshTask.setOnFailed(e -> LOGGER.error("getting sections", e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(refreshTask, "getting sections");
	}

	private synchronized void refreshWithPattern() {
		if (patternDelay != null) {
			patternDelay.stop();
		}

		if (patternField.getText().isEmpty()) {
			refreshAsync(v -> {
				init(initSectionName, initTagName);
				return null;
			});
		} else {
			patternDelay = new DelayTimeline(Duration.millis(500), e -> refreshWithPatternAsync());
			patternDelay.play();
		}
	}

	private synchronized void refreshWithPatternAsync() {
		++refreshTaskId;
		if (refreshTask != null && refreshTask.isRunning()) {
			refreshTask.cancel();
		}

		SearchPane<S, T> searchPane = null;
		if (sectionAcc.getPanes().size() != 1
				|| !(sectionAcc.getPanes().get(0) instanceof SearchPane)) {
			searchPane = new SearchPane<S, T>(tagService, showExcludeBox);
			searchPane.setOnSelectTag(selectTagHandler);
			searchPane.setOnUpdateSection(updateSectionHandler);
			searchPane.setExtItemDataFormat(extItemDataFormat);

			sectionAcc.getPanes().setAll(searchPane);
		}
		searchPane.refresh(patternField.getText(), getIncludedTags(), getExcludedTags(), itemPattern);
	}

	public void clear(final String tagName) {
		for(final TitledPane sectionPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) sectionPane.getContent();
			tagList.clear(tagName);
		}
	}

	public void init(final String sectionName, final String tagName) {
		initSectionName = sectionName;
		initTagName = tagName;

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
			unselectOthers();
			selectedTagsPane.refresh(getAllSelectedTags());
			handler.handle(event);
		};
	}

	private void unselectOthers() {
		final TitledPane expanded = sectionAcc.getExpandedPane();
		for(final TitledPane pane: sectionAcc.getPanes()) {
			if (pane != expanded) {
				@SuppressWarnings("unchecked")
				final SectionPane<S, T> sectionPane = (SectionPane<S, T>) pane;
				sectionPane.clearSelection();
			}
		}
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
		final String pattern = patternField.getText();
		final Set<T> includes = getIncludedTags();
		final Set<T> excludes = getExcludedTags();
		final boolean checkMode = isCheckMode();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final SectionPane<S, T> sectionPane = (SectionPane<S, T>) titledPane;
			if (!checkMode) {
				try {
					//TODO param toRefresh is not used
					toRefresh = tagService.getTags(sectionPane.getSection());
				} catch (final TagException e) {
					LOGGER.error("getting section count", e);
				}
			}
			sectionPane.updateCountAsync(queryCount, toRefresh, includes, excludes, itemPattern, pattern);
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
			tag = tagList.getSelectedTag();
			if (tag != null) {
				break;
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

	@SuppressWarnings("unchecked")
	public void clearSelection() {
		sectionAcc.getPanes().forEach(pane -> ((SectionPane<S, T>) pane).clearSelection());
	}
}
