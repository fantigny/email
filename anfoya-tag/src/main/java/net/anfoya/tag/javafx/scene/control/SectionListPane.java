package net.anfoya.tag.javafx.scene.control;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Title;
import net.anfoya.tag.javafx.scene.control.dnd.DndFormat;
import net.anfoya.tag.javafx.scene.control.dnd.ExtItemDropPane;
import net.anfoya.tag.javafx.scene.control.dnd.SectionDropPane;
import net.anfoya.tag.javafx.scene.control.dnd.TagDropPane;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

public class SectionListPane<S extends SimpleSection, T extends SimpleTag> extends BorderPane {
	private final TagService<S, T> tagService;
	private final DataFormat extItemDataFormat;

	private final TextField tagPatternField;
	private final Accordion sectionAcc;
	private final SelectedTagsPane<T> selectedPane;

	private Set<S> sections;
	private ChangeListener<Boolean> tagChangeListener;

	private Callback<Void, Void> updateSectionCallback;

	private boolean lazyCount = true;
	private boolean sectionDisableWhenZero = true;

	public SectionListPane(final TagService<S, T> tagService) {
		this(tagService, null);
	}

	public SectionListPane(final TagService<S, T> tagService, final DataFormat extItemDataFormat) {
		this.tagService = tagService;
		this.extItemDataFormat = extItemDataFormat;

		final BorderPane patternPane = new BorderPane();
		setTop(patternPane);

		final Title title = new Title("Tags");
		title.setPadding(new Insets(0, 10, 0, 5));
		patternPane.setLeft(title);

		tagPatternField = new TextField();
		tagPatternField.setPromptText("search");
		tagPatternField.textProperty().addListener((ChangeListener<String>) (ov, oldPattern, newPattern) -> {
			refreshWithPattern();
		});
		patternPane.setCenter(tagPatternField);
		BorderPane.setMargin(tagPatternField, new Insets(0, 5, 0, 5));

		final Button delPatternButton = new Button("X");
		delPatternButton.setOnAction(event -> tagPatternField.textProperty().set(""));
		patternPane.setRight(delPatternButton);

		sectionAcc = new Accordion();
		final StackPane stackPane = new StackPane(sectionAcc);
		stackPane.setAlignment(Pos.BOTTOM_CENTER);

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
		stackPane.setOnDragDone(event -> {
			updateSectionCallback.call(null);
		});
		setCenter(stackPane);

		selectedPane = new SelectedTagsPane<T>();
		selectedPane.setDelTagCallBack(tag -> {
			unselectTag(tag.getName());
			return null;
		});
		setBottom(selectedPane);

		BorderPane.setMargin(patternPane, new Insets(5));
		BorderPane.setMargin(stackPane, new Insets(0, 5, 0, 5));
		BorderPane.setMargin(selectedPane, new Insets(5));
	}

	public void refreshSections() {
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
				final TagList<S, T> tagList = new TagList<S, T>(tagService, section);
				tagList.setTagChangeListener(tagChangeListener);
				tagList.setExtItemDataFormat(extItemDataFormat);

				final SectionPane<S, T> sectionPane = new SectionPane<S, T>(tagService, section, tagList);
				sectionPane.setDisableWhenZero(sectionDisableWhenZero);
				sectionPane.setLazyCount(lazyCount);
				sectionPane.setOnDragDetected(event -> {
			        final ClipboardContent content = new ClipboardContent();
			        content.put(DndFormat.SECTION_DATA_FORMAT, section);
			        final Dragboard db = sectionPane.startDragAndDrop(TransferMode.ANY);
			        db.setContent(content);
				});
				sectionPane.setOnDragOver(event -> {
					if (!sectionPane.isExpanded()
							&& (event.getDragboard().hasContent(extItemDataFormat)
									|| event.getDragboard().hasContent(DndFormat.TAG_DATA_FORMAT))) {
						sectionPane.setExpanded(true);
						event.consume();
					}
				});
				sectionPane.setOnDragDone(event -> {
					refresh();
					event.consume();
				});
				sectionAcc.getPanes().add(index, sectionPane);
			}
			index++;
		}
	}

	public void refreshTags() {
		final String tagPattern = tagPatternField.getText();
		final Set<T> tags = getIncludedTags();
		for(final TitledPane pane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final SectionPane<S, T> sectionPane = (SectionPane<S, T>) pane;
			sectionPane.refresh(tags, tagPattern);
		}
	}

	public void refreshAsync() {
		final Task<Set<S>> task = new Task<Set<S>>() {
			@Override
			protected Set<S> call() throws Exception {
				return tagService.getSections();
			}
		};
		task.setOnSucceeded(event -> {
			try {
				sections = task.get();
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			refreshSections();
			refreshTags();
			selectedPane.refresh(getAllSelectedTags());
		});
		ThreadPool.getInstance().submit(task);
	}

	public void refresh() {
		try {
			sections = tagService.getSections();
		} catch (final TagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		refreshSections();
		refreshTags();
		selectedPane.refresh(getAllSelectedTags());
	}

	protected void refreshWithPattern() {
		refresh();
		if (!lazyCount) {
			tagChangeListener.changed(null, null, null);
		}
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

	public void selectTag(final SimpleSection section, final String tagName) {
		for(final TitledPane sectionPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) sectionPane.getContent();
			if (tagList.getSection().equals(section)) {
				tagList.setTagSelected(tagName, true);
				break;
			}
		}
	}

	public void setTagChangeListener(final ChangeListener<Boolean> listener) {
		tagChangeListener = (ov, oldVal, newVal) -> {
			listener.changed(ov, oldVal, newVal);
			selectedPane.refresh(getAllSelectedTags());
		};
	}

	public void setUpdateSectionCallback(final Callback<Void, Void> callback) {
		updateSectionCallback = callback;
	}

	public void expand(final SimpleSection section) {
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) titledPane.getContent();
			if (tagList.getSection().equals(section)) {
				titledPane.expandedProperty().set(true);
			}
		}
	}

	public void updateCount(final int currentCount, final Set<T> availableTags, final String namePattern) {
		final Set<T> includes = getIncludedTags();
		final Set<T> excludes = getExcludedTags();
		final String tagPattern = tagPatternField.getText();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final SectionPane<S, T> sectionPane = (SectionPane<S, T>) titledPane;
			sectionPane.updateCountAsync(currentCount, availableTags, includes, excludes, namePattern, tagPattern);
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

	public Set<T> getIncludedTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			@SuppressWarnings("unchecked")
			final TagList<S, T> tagList = (TagList<S, T>) titledPane.getContent();
			tags.addAll(tagList.getSelectedTags());
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
}