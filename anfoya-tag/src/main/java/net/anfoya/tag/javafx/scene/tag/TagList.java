package net.anfoya.tag.javafx.scene.tag;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.DataFormat;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO use tag id instead of tag name

public class TagList<S extends SimpleSection, T extends SimpleTag> extends ListView<TagListItem<T>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagList.class);

	private final TagService<S, T> tagService;

	private final S section;
	private final Map<String, TagListItem<T>> itemMap = new HashMap<String, TagListItem<T>>();

	private ChangeListener<? super Boolean> incExcListener;
	private DataFormat extItemDataFormat;

	private final AtomicBoolean refreshing = new AtomicBoolean(false);

	private Task<Integer> countTask;

	private int countTaskId;

	public TagList(final TagService<S, T> tagService, final S section) {
		this.tagService = tagService;
		this.section = section;

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setCellFactory(list -> new TagListCell<T>(extItemDataFormat));
	}

	public T getSelectedTag() {
		final TagListItem<T> item = getSelectionModel().getSelectedItem();
		return item == null? null: item.getTag();
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
		refreshing.set(true);
		final Map<String, TagListItem<T>> countedItemMap = new HashMap<String, TagListItem<T>>(itemMap);
		itemMap.clear();
		final ObservableList<TagListItem<T>> items = FXCollections.observableArrayList();
		for(final T tag: tags) {
			final TagListItem<T> item = new TagListItem<T>(tag);
			if (includes.contains(tag)) {
				item.includedProperty().set(true);
			} else if (excludes.contains(tag)) {
				item.excludedProperty().set(true);
			}
			if (incExcListener != null) {
				item.includedProperty().addListener(incExcListener);
				item.excludedProperty().addListener(incExcListener);
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
		refreshing.set(false);

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
	}

	protected synchronized void updateCountAsync(final TagListItem<T> item, final Set<T> includes, final Set<T> excludes, final String nameFilter) {
		final long taskId = ++countTaskId;
		if (countTask != null && countTask.isRunning()) {
			countTask.cancel();
		}
		countTask = new Task<Integer>() {
			@Override
			public Integer call() throws SQLException, TagException, InterruptedException {
				final T tag = item.getTag();
				final int excludeFactor = excludes.contains(tag)? -1: 1;
				@SuppressWarnings("serial")
				final Set<T> fakeIncludes = new LinkedHashSet<T>(includes) {{ add(tag); }};
				@SuppressWarnings("serial")
				final Set<T> fakeExcludes = new LinkedHashSet<T>(excludes) {{ remove(tag); }};
				return excludeFactor * tagService.getCountForTags(fakeIncludes, fakeExcludes, nameFilter);
			}
		};
		countTask.setOnFailed(event -> {
			// TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out);
		});
		countTask.setOnSucceeded(event -> {
			if (taskId != countTaskId) {
				return;
			}
			item.countProperty().set(countTask.getValue());
		});
		ThreadPool.getInstance().submitLow(countTask);
	}

	public void setOnIncExcTag(final EventHandler<ActionEvent> handler) {
		incExcListener = (ov, oldVal, newVal) -> {
			if (refreshing.get()) {
				return;
			}
			handler.handle(null);
		};
		for(final TagListItem<T> item: itemMap.values()) {
			item.includedProperty().addListener(incExcListener);
			item.excludedProperty().addListener(incExcListener);
		}
	}

	public void setOnSelectTag(final EventHandler<ActionEvent> handler) {
		getSelectionModel().selectedItemProperty().addListener((ov, oldVal, newVal) -> {
			if (refreshing.get()) {
				return;
			}
			if (!hasCheckedTag()) {
				handler.handle(null);
			}
		});
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

	public boolean hasCheckedTag() {
		for(final TagListItem<T> item: getItems()) {
			if (item.includedProperty().get() || item.excludedProperty().get()) {
				return true;
			}
		}
		return false;
	}
}
