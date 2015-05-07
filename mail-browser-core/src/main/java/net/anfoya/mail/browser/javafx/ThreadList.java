package net.anfoya.mail.browser.javafx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.model.SimpleThread.SortOrder;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagServiceException;

public class ThreadList<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread> extends ListView<H> {
	private final MailService<S, T, H> mailService;

	private final Predicate<H> nameFilter = new Predicate<H>() {
		@Override
		public boolean test(final H thread) {
			//TODO compare sender?
			return thread.getSubject().toLowerCase().contains(namePattern);
		}
	};

	private boolean refreshing;
	private Set<H> threads;

	private Set<T> tags;
	private Set<T> includes;
	private Set<T> excludes;
	private SortOrder sortOrder;
	private String namePattern;

	public ThreadList(final MailService<S, T, H> mailService) {
		this.mailService = mailService;

		this.refreshing = false;
		this.threads = new LinkedHashSet<H>();

		this.tags = new LinkedHashSet<T>();
		this.includes = new LinkedHashSet<T>();
		this.excludes = new LinkedHashSet<T>();
		this.sortOrder = SortOrder.DATE;
		this.namePattern = "";

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	public void refreshWithPattern(final String pattern) {
		final String previousPattern = namePattern;
		namePattern = pattern.toLowerCase();

		if (namePattern.contains(previousPattern)) {
			refresh();
		} else {
			final Task<Set<H>> task = new Task<Set<H>>() {
				@Override
				protected Set<H> call() throws Exception {
					return mailService.getThreads(tags, includes, excludes, namePattern);
				}
			};
			task.setOnSucceeded(event -> {
				try {
					threads = task.get();
					refresh();
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			ThreadPool.getInstance().submit(task);
		}
	}

	public void refreshWithOrder(final SortOrder order) {
		sortOrder = order;
		refresh();
	}

	public void refreshWithTags(final Set<T> tags, final Set<T> includes, final Set<T> excludes) {
		this.tags = tags;
		this.includes = includes;
		this.excludes = excludes;
		final Task<Set<H>> task = new Task<Set<H>>() {
			@Override
			protected Set<H> call() throws Exception {
				return mailService.getThreads(tags, includes, excludes, namePattern);
			}
		};
		task.setOnSucceeded(event -> {
			try {
				threads = task.get();
				refresh();
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		ThreadPool.getInstance().submit(task);
	}

	public void refresh() {
		// get previously selected indices
		final List<String> previouslySelectedIds = new ArrayList<String>();
		for(final H thread: getSelectedMovies()) {
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
		getItems().setAll(obsThreads);
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
				} catch (final TagServiceException e) {
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

	public Set<H> getSelectedMovies() {
		final List<H> selectedMovies = getSelectionModel().getSelectedItems();
		if (!selectedMovies.isEmpty() && selectedMovies.iterator().next() == null) {
			return Collections.unmodifiableSet(new LinkedHashSet<H>());
		}
		return Collections.unmodifiableSet(new LinkedHashSet<H>(selectedMovies));
	}
}
