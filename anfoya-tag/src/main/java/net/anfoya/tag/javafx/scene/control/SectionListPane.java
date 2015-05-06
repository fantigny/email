package net.anfoya.tag.javafx.scene.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import net.anfoya.javafx.scene.control.Title;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagService;
import net.anfoya.tag.service.TagServiceException;

public class SectionListPane<S extends SimpleSection, T extends SimpleTag> extends BorderPane {
	private final TagService<S, T> tagService;

	private TextField tagPatternField;
	private final Accordion sectionAcc;
	private final SelectedTagsPane<T> selectedPane;
	private final ContextMenu contextMenu;
	private final Menu moveToSectionMenu;

	private Set<S> sections;
	private ChangeListener<Boolean> tagChangeListener;

	private Callback<Void, Void> updateSectionCallback;

	private boolean lazyCounting = true;
	private boolean sectionDisableWhenZero = true;

	public SectionListPane(final TagService<S, T> tagService) {
		this.tagService = tagService;

		final BorderPane patternPane = new BorderPane();
		setTop(patternPane);

		final Title title = new Title("Tags");
		title.setPadding(new Insets(0, 10, 0, 5));
		patternPane.setLeft(title);

		tagPatternField = new TextField();
		tagPatternField.setPromptText("search");
		tagPatternField.textProperty().addListener((ChangeListener<String>) (ov, oldPattern, newPattern) -> refreshWithPattern());
		patternPane.setCenter(tagPatternField);
		BorderPane.setMargin(tagPatternField, new Insets(0, 5, 0, 5));

		final Button delPatternButton = new Button("X");
		delPatternButton.setOnAction(event -> tagPatternField.textProperty().set(""));
		patternPane.setRight(delPatternButton);

		sectionAcc = new Accordion();
		setCenter(sectionAcc);

		selectedPane = new SelectedTagsPane<T>();
		selectedPane.setDelTagCallBack(tagName -> {
			unselectTag(tagName);
			return null;
		});
		setBottom(selectedPane);

		setMargin(patternPane, new Insets(5));
		setMargin(sectionAcc, new Insets(0, 5, 0, 5));
		setMargin(selectedPane, new Insets(5));

		moveToSectionMenu = new Menu("Move to");
		final MenuItem newSectionItem = new MenuItem("Create new");
		newSectionItem.setOnAction(event -> createSection());

		contextMenu = new ContextMenu(moveToSectionMenu, newSectionItem);
	}

	public void refreshSections() {
		try {
			sections = tagService.getSections();
		} catch (final TagServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

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
				tagList.setContextMenu(contextMenu);

				final SectionPane<S, T> sectionPane = new SectionPane<S, T>(tagService, section, tagList);
				sectionPane.setDisableWhenZero(sectionDisableWhenZero);
				sectionPane.setLazyCount(lazyCounting);
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

		refreshMoveToSectionMenu();
	}

	public void refresh() {
		refreshSections();
		refreshTags();
		selectedPane.refresh(getAllSelectedTags());
	}

	protected void refreshWithPattern() {
		refresh();
		tagChangeListener.changed(null, null, null);
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

	public List<T> getAllSelectedTags() {
		final List<T> tags = new ArrayList<T>();
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

	private void refreshMoveToSectionMenu() {
		final List<MenuItem> itemList = FXCollections.observableArrayList();
		for(final S section: sections) {
			if (section.equals(SimpleSection.NO_SECTION) || section.equals(SimpleSection.TO_WATCH)) {
				continue;
			}
			final MenuItem item = new MenuItem(section.getName());
			item.setOnAction(event -> {
				contextMenu.hide();
				moveToSection(section);
			});
			itemList.add(item);
		}
		moveToSectionMenu.getItems().setAll(itemList);
	}

	private void moveToSection(final S section) {
		@SuppressWarnings("unchecked")
		final TagList<S, T> tagList = (TagList<S, T>) sectionAcc.getExpandedPane().getContent();
		final T tag = tagList.getFocusedTag();
		if (tag == null) {
			return;
		}

		final boolean selected = getIncludedTags().contains(tag);

		try {
			tagService.moveToSection(section, tag);
		} catch (final TagServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		refresh();

		if (selected) {
			selectTag(section, tag.getName());
		}

		updateSectionCallback.call(null);
	}

	private void createSection() {
		@SuppressWarnings("unchecked")
		final TagList<S, T> tagList = (TagList<S, T>) sectionAcc.getExpandedPane().getContent();
		final T tag = tagList.getSelectedTag();
		if (tag == null) {
			return;
		}

		final TextInputDialog inputDialog = new TextInputDialog();
		inputDialog.setTitle("Create new section");
		inputDialog.setContentText("Section name:");
		inputDialog.setHeaderText("");
		final Optional<String> response = inputDialog.showAndWait();

		if (!response.isPresent()) {
			return;
		}
		final String sectionName = response.get();
		if (sectionName.length() < 3) {
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setTitle("Create new section");
			alertDialog.setContentText("Section name should be a least 3 letters long.");
			alertDialog.setHeaderText("Section name is too short: " + sectionName);
			alertDialog.showAndWait();
			return;
		}

		try {
			tagService.moveToSection(tagService.addSection(sectionName), tag);
		} catch (final TagServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		refresh();
	}

	public boolean isSectionDisableWhenZero() {
		return sectionDisableWhenZero;
	}

	public void setSectionDisableWhenZero(final boolean disable) {
		this.sectionDisableWhenZero = disable;
	}

	public boolean isLazy() {
		return lazyCounting;
	}

	public void setLazy(final boolean isLazy) {
		this.lazyCounting = isLazy;
	}
}