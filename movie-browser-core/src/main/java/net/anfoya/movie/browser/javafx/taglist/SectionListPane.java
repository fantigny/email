package net.anfoya.movie.browser.javafx.taglist;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import net.anfoya.movie.browser.model.Section;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.movie.browser.service.TagService;

public class SectionListPane extends BorderPane {
	private final TagService tagService;

	private TextField tagPatternField;
	private final Accordion sectionAcc;
	private final SelectedTagsPane selectedPane;
	private final ContextMenu contextMenu;
	private final Menu moveToSectionMenu;

	private Set<Section> sections;
	private ChangeListener<Boolean> tagChangeListener;

	private Callback<Void, Void> updateSectionCallback;

	public SectionListPane(final TagService tagService) {
		this.tagService = tagService;

		final BorderPane patternPane = new BorderPane();
		setTop(patternPane);

		final Title title = new Title("Tags");
		title.setPadding(new Insets(0, 10, 0, 5));
		patternPane.setLeft(title);

		tagPatternField = new TextField();
		tagPatternField.setPromptText("search");
		tagPatternField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> ov, final String oldPattern, final String newPattern) {
				refreshWithPattern();
			}
		});
		patternPane.setCenter(tagPatternField);
		BorderPane.setMargin(tagPatternField, new Insets(0, 5, 0, 5));

		final Button delPatternButton = new Button("X");
		delPatternButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				tagPatternField.textProperty().set("");
			}
		});
		patternPane.setRight(delPatternButton);

		sectionAcc = new Accordion();
		setCenter(sectionAcc);

		selectedPane = new SelectedTagsPane();
		selectedPane.setDelTagCallBack(new Callback<String, Void>() {
			@Override
			public Void call(final String tagName) {
				unselectTag(tagName);
				return null;
			}
		});
		setBottom(selectedPane);

		setMargin(patternPane, new Insets(5));
		setMargin(sectionAcc, new Insets(0, 5, 0, 5));
		setMargin(selectedPane, new Insets(5));

		moveToSectionMenu = new Menu("Move to");
		final MenuItem newSectionItem = new MenuItem("Create new");
		newSectionItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				createSection();
			}
		});

		contextMenu = new ContextMenu(moveToSectionMenu, newSectionItem);
	}

	public void refreshSections() {
		sections = tagService.getSections();

		// delete sections
		final Set<Section> existingSections = new LinkedHashSet<Section>();
		for(final Iterator<TitledPane> i = sectionAcc.getPanes().iterator(); i.hasNext();) {
			final TagList tagList = (TagList) i.next().getContent();
			if (sections.contains(tagList.getSection())) {
				existingSections.add(tagList.getSection());
			} else {
				i.remove();
			}
		}

		// add new sections
		int index = 0;
		for(final Section section: sections) {
			if (!existingSections.contains(section)) {
				final TagList tagList = new TagList(tagService, section);
				tagList.setTagChangeListener(tagChangeListener);
				tagList.setContextMenu(contextMenu);

				final SectionPane sectionPane = new SectionPane(tagService, section, tagList);
				sectionAcc.getPanes().add(index, sectionPane);
			}
			index++;
		}
	}

	public void refreshTags() {
		final String tagPattern = tagPatternField.getText();
		final Set<Tag> tags = getIncludedTags();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			final SectionPane sectionPane = (SectionPane) titledPane;
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
			final TagList tagList = (TagList) sectionPane.getContent();
			if (tagList.contains(tagName)) {
				tagList.setTagSelected(tagName, false);
				break;
			}
		}
	}

	public void selectTag(final Section section, final String tagName) {
		for(final TitledPane sectionPane: sectionAcc.getPanes()) {
			final TagList tagList = (TagList) sectionPane.getContent();
			if (tagList.getSection().equals(section)) {
				tagList.setTagSelected(tagName, true);
				break;
			}
		}
	}

	public void setTagChangeListener(final ChangeListener<Boolean> listener) {
		tagChangeListener = new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
				listener.changed(ov, oldVal, newVal);
				selectedPane.refresh(getAllSelectedTags());
			}
		};
	}

	public void setUpdateSectionCallback(final Callback<Void, Void> callback) {
		updateSectionCallback = callback;
	}

	public void expand(final Section section) {
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			final TagList tagList = (TagList) titledPane.getContent();
			if (tagList.getSection().equals(section)) {
				titledPane.expandedProperty().set(true);
			}
		}
	}

	public void updateMovieCount(final int currentCount, final Set<Tag> availableTags, final String namePattern) {
		final Set<Tag> includes = getIncludedTags();
		final Set<Tag> excludes = getExcludedTags();
		final String tagPattern = tagPatternField.getText();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			final SectionPane sectionPane = (SectionPane) titledPane;
			sectionPane.updateMovieCountAsync(currentCount, availableTags, includes, excludes, namePattern, tagPattern);
		}
	}

	public Set<Tag> getAllTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			final TagList tagList = (TagList) titledPane.getContent();
			tags.addAll(tagList.getTags());
		}

		return Collections.unmodifiableSet(tags);
	}

	public Set<Tag> getAllSelectedTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		tags.addAll(getIncludedTags());
		tags.addAll(getExcludedTags());
		return tags;
	}

	public Set<Tag> getIncludedTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			final TagList tagList = (TagList) titledPane.getContent();
			tags.addAll(tagList.getSelectedTags());
		}

		return Collections.unmodifiableSet(tags);
	}

	public Set<Tag> getExcludedTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final TitledPane titledPane: sectionAcc.getPanes()) {
			final TagList tagList = (TagList) titledPane.getContent();
			tags.addAll(tagList.getExcludedTags());
		}
		return Collections.unmodifiableSet(tags);
	}

	private void refreshMoveToSectionMenu() {
		final List<MenuItem> itemList = FXCollections.observableArrayList();
		for(final Section section: sections) {
			if (section.equals(Section.NO_SECTION) || section.equals(Section.TO_WATCH)) {
				continue;
			}
			final MenuItem item = new MenuItem(section.getName());
			item.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					contextMenu.hide();
					moveToSection(section);
				}
			});
			itemList.add(item);
		}
		moveToSectionMenu.getItems().setAll(itemList);
	}

	private void moveToSection(final Section section) {
		final TagList tagList = (TagList) sectionAcc.getExpandedPane().getContent();
		final Tag tag = tagList.getFocusedTag();
		if (tag == null) {
			return;
		}

		final boolean selected = getIncludedTags().contains(tag);

		tagService.addToSection(tag.copyWithSection(section.getName()));
		refresh();

		if (selected) {
			selectTag(section, tag.getName());
		}

		updateSectionCallback.call(null);
	}

	private void createSection() {
		final TagList tagList = (TagList) sectionAcc.getExpandedPane().getContent();
		final Tag tag = tagList.getSelectedTag();
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

		tagService.addToSection(tag.copyWithSection(sectionName));
		refresh();
	}
}