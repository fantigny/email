package net.anfoya.tag.javafx.scene.tag;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.DataFormat;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

//TODO use tag id instead of tag name

public class TagList<S extends SimpleSection, T extends SimpleTag> extends ListView<TagListItem<T>> {
	private final TagService<S, T> tagService;

	private final S section;
	private final Map<String, TagListItem<T>> itemMap = new HashMap<String, TagListItem<T>>();

	private ChangeListener<Boolean> tagChangeListener;
	private DataFormat extItemDataFormat;

	public TagList(final TagService<S, T> tagService, final S section) {
		this.tagService = tagService;
		this.section = section;

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setCellFactory(list -> new TagListCell<T>(extItemDataFormat));
	}

	public T getSelectedTag() {
		T tag = null;
		if (!getSelectionModel().isEmpty()) {
			tag = getSelectionModel().getSelectedItem().getTag();
		}
		return tag;
	}

	public Set<T> getTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		for(final TagListItem<T> item: getItems()) {
			tags.add(item.getTag());
		}

		return Collections.unmodifiableSet(tags);
	}

	public Set<T> getIncludedTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		for(final TagListItem<T> item: getItems()) {
			if (item.includedProperty().get()) {
				tags.add(item.getTag());
			}
		}

		return Collections.unmodifiableSet(tags);
	}

	public Set<T> getExcludedTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		for(final TagListItem<T> item: getItems()) {
			if (item.excludedProperty().get()) {
				tags.add(item.getTag());
			}
		}

		return Collections.unmodifiableSet(tags);
	}

	public void refresh(final Set<T> includes, final Set<T> excludes, final String tagPattern) {
		// get all tags
		Set<T> tags;
		try {
			tags = tagService.getTags(section, tagPattern);
		} catch (final TagException e) {
			//TODO display exception
			e.printStackTrace();
			return;
		}
		
		// get selected index
		final int selectedIndex = getSelectionModel().getSelectedIndex();

		// build items map and restore selection
		final Map<String, TagListItem<T>> countedItemMap = this.itemMap;
		itemMap.clear();
		final ObservableList<TagListItem<T>> items = FXCollections.observableArrayList();
		for(final T tag: tags) {
			final TagListItem<T> item = new TagListItem<T>(tag);
			if (includes.contains(tag)) {
				item.includedProperty().set(true);
			} else if (excludes.contains(tag)) {
				item.excludedProperty().set(true);
			}
			if (tagChangeListener != null) {
				item.includedProperty().addListener(tagChangeListener);
				item.excludedProperty().addListener(tagChangeListener);
			}
			if (countedItemMap.containsKey(tag.getName())) {
				item.countProperty().set(countedItemMap.get(tag.getName()).countProperty().get());
			}

			items.add(item);
			itemMap.put(tag.getName(), item);
		}

		// display
		getItems().setAll(items);

		// restore selection
		if (selectedIndex != -1) {
			getSelectionModel().selectIndices(selectedIndex);
		}
	}

	public void updateCount(final int currentCount, final Set<T> availableTags, final Set<T> includes, final Set<T> excludes, final String namePattern) {
		for(final TagListItem<T> item: getItems()) {
			if (availableTags.contains(item.getTag()) || item.excludedProperty().get()) {
				if (item.includedProperty().get()) {
					item.countProperty().set(currentCount);
				} else {
					// request count for available tags
					updateCountAsync(item, includes, excludes, namePattern);
				}
			} else {
				if (includes.isEmpty() && excludes.isEmpty()) {
					updateCountAsync(item, includes, excludes, namePattern);
				} else {
					item.countProperty().set(0);
				}
			}
		}
		forceRepaint();
	}

	protected void updateCountAsync(final TagListItem<T> item, final Set<T> includes, final Set<T> excludes, final String nameFilter) {
		final Task<Integer> task = new Task<Integer>() {
			@Override
			public Integer call() throws SQLException, TagException, InterruptedException {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
				final T tag = item.getTag();
				final int excludeFactor = excludes.contains(tag)? -1: 1;
				@SuppressWarnings("serial")
				final Set<T> fakeIncludes = new LinkedHashSet<T>(includes) {{ add(tag); }};
				@SuppressWarnings("serial")
				final Set<T> fakeExcludes = new LinkedHashSet<T>(excludes) {{ remove(tag); }};
				return excludeFactor * tagService.getCountForTags(fakeIncludes, fakeExcludes, nameFilter);
			}
		};
		task.setOnFailed(event -> {
			// TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out);
		});
		task.setOnSucceeded(event -> {
			item.countProperty().set(task.getValue());
			forceRepaint();
		});
		ThreadPool.getInstance().submitLow(task);
	}

	public void setTagChangeListener(final ChangeListener<Boolean> listener) {
		getSelectionModel().selectedItemProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal != null) {
				listener.changed(null, null, null);
			}
		});
		tagChangeListener = listener;
	}

	// TODO: find a proper way
	private void forceRepaint() {
		setCellFactory(param -> new TagListCell<T>(extItemDataFormat));

//		setCellFactory(new Callback<ListView<TagListItem<T>>, ListCell<TagListItem<T>>>() {
//			@Override
//			public ListCell<TagListItem<T>> call(final ListView<TagListItem<T>> param) {
//				return new TagListCell<T>();
//			}
//		});
	}

	public void setTagSelected(final String tagName, final boolean selected) {
		if (itemMap.containsKey(tagName)) {
			getSelectionModel().select(itemMap.get(tagName));
			final TagListItem<T> item = itemMap.get(tagName);
			item.includedProperty().set(selected);
			if (!selected) {
				item.excludedProperty().set(false);
			}
		}
	}

	public S getSection() {
		return section;
	}

	public boolean contains(final String tagName) {
		return itemMap.containsKey(tagName);
	}

	public TagListItem<T> getSectionItem() {
		return itemMap.get(section.getName());
	}

	public boolean isStandAloneSectionTag() {
		return getSectionItem() != null && getItems().size() == 1;
	}

	public void setExtItemDataFormat(final DataFormat dataFormat) {
		this.extItemDataFormat = dataFormat;
	}
}
