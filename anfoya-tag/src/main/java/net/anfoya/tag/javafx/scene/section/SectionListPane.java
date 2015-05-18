package net.anfoya.tag.javafx.scene.section;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Title;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.javafx.scene.dnd.ExtItemDropPane;
import net.anfoya.tag.javafx.scene.tag.TagDropPane;
import net.anfoya.tag.javafx.scene.tag.TagList;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class SectionListPane<S extends SimpleSection, T extends SimpleTag> extends BorderPane {
	private final TagService<S, T> tagService;
	private final DataFormat extItemDataFormat;

	private final TextField tagPatternField;
	private final Accordion sectionAcc;
	private final SelectedTagsPane<T> selectedPane;

	private Set<S> sections;
	private EventHandler<ActionEvent> selectTagHandler;
	private EventHandler<ActionEvent> updateSectionHandler;

	private boolean lazyCount;
	private boolean sectionDisableWhenZero;
	private Set<T> availableTags;
	private Set<T> excludedTags;
	private Set<T> includedTags;
	private int currentCount;
	private String namePattern;
	private String tagPattern;
	private Task<Set<S>> refreshTask;
	private int refreshTaskId;

	public SectionListPane(final TagService<S, T> tagService) {
		this(tagService, null);
		lazyCount = true;
		sectionDisableWhenZero = true;
	}

	public SectionListPane(final TagService<S, T> tagService, final DataFormat extItemDataFormat) {
		this.tagService = tagService;
		this.extItemDataFormat = extItemDataFormat;

		tagPatternField = new TextField();
		tagPatternField.prefWidthProperty().bind(widthProperty());
		tagPatternField.setPromptText("search");
		tagPatternField.textProperty().addListener((ChangeListener<String>) (ov, oldPattern, newPattern) -> {
			refreshWithTagPattern();
		});

		final HBox patternBox = new HBox(5, new Title("Label"), tagPatternField);
		patternBox.setPadding(new Insets(0 , 5, 0, 5));
		setTop(patternBox);

		/* TODO: text field with stacked pane for reset button
		final Button delPatternButton = new Button("X");
		delPatternButton.setOnAction(event -> tagPatternField.textProperty().set(""));
		patternPane.setRight(delPatternButton);
		*/

		sectionAcc = new Accordion();
		final StackPane stackPane = new StackPane(sectionAcc);
		stackPane.setAlignment(Pos.BOTTOM_CENTER);
		stackPane.setPadding(new Insets(5, 0, 5, 0));

		final ExtItemDropPane<T> extItemDropPane = new ExtItemDropPane<T>(tagService, extItemDataFormat);
		extItemDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		final SectionDropPane<S> sectionDropPane = new SectionDropPane<S>(tagService);
		sectionDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		final TagDropPane<S, T> tagDropPane = new TagDropPane<S, T>(tagService);
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
			event.consume();
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
			event.consume();
		});
		stackPane.setOnDragDone(event -> {
			if (event.isDropCompleted()) {
				updateSectionHandler.handle(null);
			}
			event.consume();
		});
		setCenter(stackPane);

		selectedPane = new SelectedTagsPane<T>();
		selectedPane.setDelTagCallBack(tag -> {
			unselectTag(tag.getName());
			return null;
		});
		setBottom(selectedPane);
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
				sectionPane.setExtItemDataFormat(extItemDataFormat);
				sectionPane.setOnDragDone(event -> {
					if (event.isDropCompleted()) {
						updateSectionHandler.handle(null);
					}
					event.consume();
				});
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
			sectionPane.refresh(includes, excludes, tagPattern);
		}
	}

	public void refreshWithTags(final Set<T> availableTags, final int currentCount, final boolean lazy) {
		this.availableTags = availableTags;
		this.currentCount = currentCount;
		if (getIncludedTags().equals(includedTags)
				&& getExcludedTags().equals(excludedTags)) {
			return;
		}

		updateCount();
	}

	public void refreshWithPattern(final String namePattern) {
		this.namePattern = namePattern;
		updateCount();
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
			selectedPane.refresh(getAllSelectedTags());
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

	protected void refreshWithTagPattern() {
		throw new NotImplementedException();
	}

	public void unselectTag(final String tagName) {
		for(final TitledPane sectionPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) sectionPane.getContent();
			if (tagList.contains(tagName)) {
				tagList.setTagSelected(tagName, false);
				break;
			}
		}
	}

	public void selectTag(final String sectionName, final String tagName) {
		for(final TitledPane sectionPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) sectionPane.getContent();
			if (tagList.getSection().getName().equals(sectionName)) {
				tagList.setTagSelected(tagName, true);
				sectionPane.setExpanded(true);
				break;
			}
		}
	}

	public void setOnSelectTag(final EventHandler<ActionEvent> handler) {
		selectTagHandler = handler;
	}

	public void setOnUpdateSection(final EventHandler<ActionEvent> handler) {
		updateSectionHandler = handler;
	}

	public void updateCount() {
		includedTags = getIncludedTags();
		excludedTags = getExcludedTags();
		tagPattern = tagPatternField.getText();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final SectionPane<S, T> sectionPane = (SectionPane<S, T>) titledPane;
			sectionPane.updateCountAsync(currentCount, availableTags, includedTags, excludedTags, namePattern, tagPattern);
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

	public boolean isCheckMode() {
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
}
