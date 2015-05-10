package net.anfoya.mail.browser.javafx.threadlist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.model.SimpleThread.SortOrder;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadList<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread> extends ListView<H> {
	private final MailService<S, T, H, ? extends SimpleMessage> mailService;
	private final AtomicLong taskId = new AtomicLong();

	private boolean refreshing;
	private Set<H> threads;

	private Set<T> tags;
	private Set<T> includes;
	private Set<T> excludes;
	private SortOrder sortOrder;
	private String namePattern;

	//TODO enhance
	private final Predicate<H> nameFilter = thread -> thread.getSubject().toLowerCase().contains(namePattern);

	public ThreadList(final MailService<S, T, H, ? extends SimpleMessage> mailService) {
		this.mailService = mailService;

		this.refreshing = false;
		this.threads = new LinkedHashSet<H>();

		this.tags = new LinkedHashSet<T>();
		this.includes = new LinkedHashSet<T>();
		this.excludes = new LinkedHashSet<T>();
		this.sortOrder = SortOrder.DATE;
		this.namePattern = "";

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setCellFactory(new Callback<ListView<H>, ListCell<H>>() {
			@Override
			public ListCell<H> call(final ListView<H> param) {
				return new ThreadListCell<H>();
			}
		});
	}

	public void refreshWithPattern(final String pattern) {
		final String previousPattern = namePattern;
		namePattern = pattern.toLowerCase();

		if (namePattern.contains(previousPattern)) {
			refresh();
		} else {
			refresh(tags, includes, excludes);
		}
	}

	public void refreshWithOrder(final SortOrder order) {
		sortOrder = order;
		load();
	}

	public void refreshWithTags(final Set<T> tags) {
		this.tags = tags;
		load();
	}

	public void refresh(final Set<T> tags, final Set<T> includes, final Set<T> excludes) {
		this.tags = tags;
		this.includes = includes;
		this.excludes = excludes;
		load();
	}

	protected void load() {
		final long id = this.taskId.incrementAndGet();
		final Task<Set<H>> task = new Task<Set<H>>() {
			@Override
			protected Set<H> call() throws Exception {
				return mailService.getThreads(tags, includes, excludes, namePattern);
			}
		};
		task.setOnSucceeded(event -> {
			try {
				threads = task.get();
				if (id == taskId.get()) {
					refresh();
				}
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		ThreadPool.getInstance().submit(task);
	}

	private void refresh() {
		// get previously selected indices
		final List<String> previouslySelectedIds = new ArrayList<String>();
		for(final H thread: getSelectedThreads()) {
			previouslySelectedIds.add(thread.getId());
		}

		// get list
		ObservableList<H> obsThreads = FXCollections.observableArrayList(threads);

		// filter
		obsThreads = FXCollections.observableArrayList(obsThreads.filtered(nameFilter));

		// sort
		Collections.sort(obsThreads, sortOrder.getComparator());

		// find selected indices in new list
		final int[] indices = new int[previouslySelectedIds.size()];
		Arrays.fill(indices, -1);
		int listIndex = 0, arrayIndex = 0;
		for(final H thread: obsThreads) {
			if (previouslySelectedIds.contains(thread.getId())) {
				indices[arrayIndex] = listIndex;
				arrayIndex++;
			}
			listIndex++;
		}

		// display
		refreshing = !previouslySelectedIds.isEmpty() && indices.length > 0 && indices[0] != -1;
		getItems().setAll(threads);
		refreshing = false;

		// restore selection
		if (indices.length > 0 && indices[0] != -1) {
			getSelectionModel().selectIndices(indices[0], indices);
		}
	}

	public Set<T> getThreadsTags() {
		// return all tags available from all threads
		final Set<T> tags = new LinkedHashSet<T>();
		for(final H thread: getItems()) {
			for(final String id: thread.getTagIds()) {
				try {
					tags.add(mailService.getTag(id));
				} catch (final MailException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return Collections.unmodifiableSet(tags);
	}

	public boolean isRefreshing() {
		return refreshing;
	}

	public Set<H> getSelectedThreads() {
		final List<H> selectedThreads = getSelectionModel().getSelectedItems();
		if (!selectedThreads.isEmpty() && selectedThreads.iterator().next() == null) {
			return Collections.unmodifiableSet(new LinkedHashSet<H>());
		}
		return Collections.unmodifiableSet(new LinkedHashSet<H>(selectedThreads));
	}
}
