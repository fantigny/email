package net.anfoya.tag.javafx.scene.control;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.tag.model.Section;
import net.anfoya.tag.model.Tag;
import net.anfoya.tag.service.TagService;
import net.anfoya.tag.service.TagServiceException;

public class TagList extends ListView<TagListItem> {
	private final TagService tagService;

	private final Section section;
	private final Map<String, TagListItem> itemMap = new HashMap<String, TagListItem>();

	private ChangeListener<Boolean> tagChangeListener;

	public TagList(final TagService tagService, final Section section) {
		this.tagService = tagService;
		this.section = section;

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setCellFactory(list -> new TagListCell());
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

	public Set<Tag> getTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final TagListItem item: getItems()) {
			tags.add(item.getTag());
		}

		return Collections.unmodifiableSet(tags);
	}

	public Set<Tag> getSelectedTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final TagListItem item: getItems()) {
			if (item.includedProperty().get()) {
				tags.add(item.getTag());
			}
		}

		return Collections.unmodifiableSet(tags);
	}

	public Set<Tag> getExcludedTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final TagListItem item: getItems()) {
			if (item.excludedProperty().get()) {
				tags.add(item.getTag());
			}
		}

		return Collections.unmodifiableSet(tags);
	}

	public void refresh(final Set<Tag> selectedTags, final String tagPattern) {
		// get all tags
		List<Tag> tags;
		try {
			tags = tagService.getTags(section, tagPattern);
		} catch (final TagServiceException e) {
			//TODO display exception
			e.printStackTrace();
			return;
		}

		// build items map and restore selection
		itemMap.clear();
		final ObservableList<TagListItem> items = FXCollections.observableArrayList();
		for(final Tag tag: tags) {
			final TagListItem item = new TagListItem(tag);
			if (selectedTags.contains(tag)) {
				item.includedProperty().set(true);
			}
			if (tagChangeListener != null) {
				item.includedProperty().addListener(tagChangeListener);
				item.excludedProperty().addListener(tagChangeListener);
			}
			items.add(item);
			itemMap.put(tag.getName(), item);
		}

		// display
		setItems(items);
	}

	public void updateCount(final int currentCount, final List<Tag> availableTags, final Set<Tag> includes, final Set<Tag> excludes, final String namePattern) {
		for(final TagListItem item: getItems()) {
			if (availableTags.contains(item.getTag()) || item.excludedProperty().get()) {
				if (item.includedProperty().get()) {
					item.countProperty().set(currentCount);
				} else {
					// request count for available tags
					updateCountAsync(item, includes, excludes, namePattern);
				}
			} else {
				item.countProperty().set(0);
			}
		}
		forceRepaint();
	}

	protected void updateCountAsync(final TagListItem item, final Set<Tag> includes, final Set<Tag> excludes, final String nameFilter) {
		final Task<Integer> task = new Task<Integer>() {
			@Override
			public Integer call() throws SQLException {
				final Tag tag = item.getTag();
				final int excludeFactor = excludes.contains(tag)? -1: 1;
				final Set<Tag> fakeIncludes = new LinkedHashSet<Tag>(includes);
				fakeIncludes.add(tag);
				final Set<Tag> fakeExcludes = new LinkedHashSet<Tag>(excludes);
				fakeExcludes.remove(tag);
				return excludeFactor * tagService.getCount(fakeIncludes, fakeExcludes, nameFilter);
			}
		};
		task.setOnSucceeded(event -> {
			item.countProperty().set((int) event.getSource().getValue());
			forceRepaint();
		});
		ThreadPool.getInstance().submit(task);
	}

	public void setTagChangeListener(final ChangeListener<Boolean> listener) {
		tagChangeListener = listener;
	}

	// TODO: find a proper way
	private void forceRepaint() {
		setCellFactory(list -> new TagListCell());
	}

	public void setTagSelected(final String tagName, final boolean selected) {
		if (itemMap.containsKey(tagName)) {
			final TagListItem item = itemMap.get(tagName);
			item.includedProperty().set(selected);
			if (!selected) {
				item.excludedProperty().set(false);
			}
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
