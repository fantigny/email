package net.anfoya.tag.javafx.scene.tag;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

//TODO use tag id instead of tag name

public class TagList<S extends SimpleSection, T extends SimpleTag> extends ListView<TagListItem<T>> {
//	private static final Logger LOGGER = LoggerFactory.getLogger(TagList.class);

	private final TagService<S, T> tagService;

	private final S section;
	private final Map<String, TagListItem<T>> itemMap = new HashMap<String, TagListItem<T>>();

	private ChangeListener<? super Boolean> incExcListener;

	private boolean refreshing;
	private boolean initializing;

	private int countTaskId;
	private final Set<Task<Integer>> updateCountTasks;

	private int initTaskId;
	private Task<Boolean> initTask;

	public TagList(final TagService<S, T> tagService, final S section) {
		this.tagService = tagService;
		this.section = section;

		updateCountTasks = new HashSet<Task<Integer>>();

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setCellFactory(list -> new TagListCell<T>());
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

	public synchronized void refresh(final Set<T> includes, final Set<T> excludes, final String tagPattern) {
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
		refreshing = true;
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
		refreshing = false;

		initThisTag();
	}

	private synchronized void initThisTag() {
		final long taskId = ++initTaskId;
		if (initTask != null && initTask.isRunning()) {
			initTask.cancel();
		}

		TagListItem<T> item = null;
		for(final Iterator<TagListItem<T>> i=getItems().iterator(); i.hasNext();) {
			item = i.next();
			if (item.getTag().getName().equals(SimpleTag.THIS_NAME)) {
				break;
			} else {
				item = null;
			}
		}
		final TagListItem<T> thisItem = item;
		if (thisItem == null) {
			return;
		}
		getItems().remove(thisItem);

		initTask = new Task<Boolean>() {
			@Override
			public Boolean call() throws SQLException, TagException, InterruptedException {
				@SuppressWarnings("serial")
				final HashSet<T> includes = new HashSet<T>() {{ add(thisItem.getTag()); }};
				return 0 == tagService.getCountForTags(includes, new HashSet<T>(), "");
			}
		};
		initTask.setOnFailed(event -> {
			// TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out);
		});
		initTask.setOnSucceeded(event -> {
			if (taskId != initTaskId) {
				return;
			}
			if (!initTask.getValue()) {
				initializing = true;
				getItems().add(0, thisItem);
				initializing = false;
			}
		});
		ThreadPool.getInstance().submitLow(initTask);
	}

	public synchronized void updateCount(final int currentCount, final Set<T> availableTags, final Set<T> includes, final Set<T> excludes, final String namePattern) {
		final long taskId = ++countTaskId;
		if (!updateCountTasks.isEmpty()) {
			for(final Task<Integer> t: updateCountTasks) {
				if (t.isRunning()) {
					t.cancel();
				}
			}
		}
		updateCountTasks.clear();

		for(final TagListItem<T> item: getItems()) {
			if (availableTags.contains(item.getTag()) || item.excludedProperty().get()) {
				if (item.includedProperty().get()) {
					item.countProperty().set(currentCount);
				} else {
					// request count for available tags
					updateCountTasks.add(updateCountAsync(item, includes, excludes, namePattern, taskId));
				}
			} else {
				if (includes.isEmpty() && excludes.isEmpty()) {
					updateCountTasks.add(updateCountAsync(item, includes, excludes, namePattern, taskId));
				} else {
					item.countProperty().set(0);
				}
			}
		}
	}

	private Task<Integer> updateCountAsync(final TagListItem<T> item, final Set<T> includes, final Set<T> excludes, final String nameFilter, final long taskId) {
		final Task<Integer> task = new Task<Integer>() {
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
		task.setOnFailed(event -> {
			// TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out);
		});
		task.setOnSucceeded(event -> {
			if (taskId != countTaskId) {
				return;
			}
			item.countProperty().set(task.getValue());
		});
		ThreadPool.getInstance().submitLow(task);

		return task;
	}

	public void setOnIncExcTag(final EventHandler<ActionEvent> handler) {
		incExcListener = (ov, oldVal, newVal) -> {
			if (refreshing || initializing) {
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
			if (refreshing || initializing) {
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

	public boolean contains(final T tag) {
		return itemMap.containsKey(tag.getName());
	}

	public TagListItem<T> getSectionItem() {
		return itemMap.get(section.getName());
	}

	public boolean isStandAloneSectionTag() {
		return getSectionItem() != null && getItems().size() == 1;
	}

	public void setExtItemDataFormat(final DataFormat dataFormat) {
		setCellFactory(list -> new TagListCell<T>(dataFormat));
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
