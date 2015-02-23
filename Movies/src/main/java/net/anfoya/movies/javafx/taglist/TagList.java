package net.anfoya.movies.javafx.taglist;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.movies.model.Section;
import net.anfoya.movies.model.Tag;
import net.anfoya.movies.service.TagService;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;

public class TagList extends ListView<TagListItem> {
	private final TagService tagService;

	private final Section section;
	private final Map<String, TagListItem> itemMap = new HashMap<String, TagListItem>();
	private final Set<ChangeListener<Boolean>> tagListeners = new LinkedHashSet<ChangeListener<Boolean>>();

	public TagList(final TagService tagService, final Section section) {
		this.tagService = tagService;
		this.section = section;

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setCellFactory(new Callback<ListView<TagListItem>, ListCell<TagListItem>>() {
			@Override
			public ListCell<TagListItem> call(final ListView<TagListItem> list) {
				return new CheckBoxCell();
			}
		});
	}

	public Tag getSelectedTag() {
		Tag tag = null;
		if (!getSelectionModel().isEmpty()) {
			tag = getSelectionModel().getSelectedItem().getTag();
		}
		return tag;
	}

	public Tag getFocusedTag() {
		final TagListItem item = getFocusModel().getFocusedItem();
		if (item == null) {
			return null;
		}

		return item.getTag();
	}

	public Set<Tag> getSelectedTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final TagListItem item: getItems()) {
			if (item.isSelected()) {
				tags.add(item.getTag());
			}
		}

		return Collections.unmodifiableSet(tags);
	}

	public void refresh(final Set<Tag> selectedTags) {
		// get all tags
		final Set<Tag> tags = tagService.getTags(section);

		// build items map and restore selection
		itemMap.clear();
		final ObservableList<TagListItem> items = FXCollections.observableArrayList();
		for(final Tag tag: tags) {
			final TagListItem item = new TagListItem(tag);
			if (selectedTags.contains(tag)) {
				item.selectedProperty().set(true);
			}
			for(final ChangeListener<Boolean> listener: tagListeners) {
				item.selectedProperty().addListener(listener);
			}
			items.add(item);
			itemMap.put(tag.getName(), item);
		}

		// display
		setItems(items);
	}

	public void updateMovieCount(final int currentCount, final Set<Tag> availableTags, final Set<Tag> selectedTags, final String namePattern) {
		getItems().forEach(new Consumer<TagListItem>() {
			@Override
			public void accept(final TagListItem item) {
				if (availableTags.contains(item.getTag())) {
					if (item.isSelected()) {
						item.movieCountProperty().set(currentCount);
					} else {
						// request count for available tags
						updateMovieCount(item, selectedTags, namePattern);
					}
				} else {
					item.movieCountProperty().set(0);
				}
			}
		});
		forceRepaint();
	}

	private void updateMovieCount(final TagListItem item, final Set<Tag> selectedTags, final String namePattern) {
		final Task<Integer> task = new Task<Integer>() {
			@Override
			public Integer call() throws SQLException {
				final Set<Tag> fakeSelectedTags = new LinkedHashSet<Tag>(selectedTags);
				fakeSelectedTags.add(item.getTag());
				return tagService.getMovieCount(fakeSelectedTags, namePattern);
			}
		};
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(final WorkerStateEvent event) {
				item.movieCountProperty().set((int) event.getSource().getValue());
				forceRepaint();
			}
		});
		ThreadPool.getInstance().submit(task);
	}

	public void addTagChangeListener(final ChangeListener<Boolean> changeListener) {
		tagListeners.add(changeListener);
	}

	// TODO: find a proper way
	private void forceRepaint() {
		setCellFactory(new Callback<ListView<TagListItem>, ListCell<TagListItem>>() {
			@Override
			public ListCell<TagListItem> call(final ListView<TagListItem> list) {
				return new CheckBoxCell();
			}
		});
	}

	public void setTagSelected(final String tagName, final boolean selected) {
		if (itemMap.containsKey(tagName)) {
			final TagListItem item = itemMap.get(tagName);
			item.selectedProperty().set(selected);
		}
	}

	public Section getSection() {
		return section;
	}

	public boolean contains(final String tagName) {
		return itemMap.containsKey(tagName);
	}

	protected TagListItem getSectionItem() {
		return itemMap.get(section.getName());
	}

	protected boolean isStandAloneSectionTag() {
		return getSectionItem() != null && getItems().size() == 1;
	}
}
