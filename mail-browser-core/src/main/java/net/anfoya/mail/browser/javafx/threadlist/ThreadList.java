package net.anfoya.mail.browser.javafx.threadlist;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.model.SimpleThread.SortOrder;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadList<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread> extends ListView<H> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadList.class);

	private final MailService<S, T, H, ? extends SimpleMessage> mailService;

	private boolean refreshing;
	private Set<H> threads;

	private Set<T> includes;
	private Set<T> excludes;
	private SortOrder sortOrder;
	private String namePattern;

	//TODO enhance
	private final Predicate<H> nameFilter = thread -> thread.getSubject().toLowerCase().contains(namePattern);
	private EventHandler<ActionEvent> loadHandler;

	private long loadTaskId;
	private Task<Set<H>> loadTask;

	public ThreadList(final MailService<S, T, H, ? extends SimpleMessage> mailService) {
		this.mailService = mailService;

		this.refreshing = false;
		this.threads = new LinkedHashSet<H>();

		this.includes = new LinkedHashSet<T>();
		this.excludes = new LinkedHashSet<T>();
		this.sortOrder = SortOrder.DATE;
		this.namePattern = "";

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setCellFactory(param -> new ThreadListCell<H>());
	}

	public void refreshWithPattern(final String pattern) {
		final String previousPattern = namePattern;
		namePattern = pattern.toLowerCase();

		if (namePattern.contains(previousPattern)) {
			refresh();
		} else {
			refresh(includes, excludes);
		}
	}

	public void refreshWithOrder(final SortOrder order) {
		sortOrder = order;
		load();
	}

	public void refresh(final Set<T> includes, final Set<T> excludes) {
		this.includes = includes;
		this.excludes = excludes;
		load();
	}

	protected synchronized void load() {
		final long taskId = ++loadTaskId;
		if (loadTask != null && loadTask.isRunning()) {
			loadTask.cancel();
		}
		loadTask = new Task<Set<H>>() {
			@Override
			protected Set<H> call() throws InterruptedException, MailException {
				LOGGER.debug("loading for includes {}, excludes {}, pattern: {}", includes, excludes, namePattern);
				return mailService.getThreads(includes, excludes, namePattern);
			}
		};
		loadTask.setOnFailed(event -> {
			// TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out);
		});
		loadTask.setOnSucceeded(event -> {
			if (taskId != loadTaskId) {
				return;
			}
			threads = loadTask.getValue();
			refresh();
		});
		ThreadPool.getInstance().submitHigh(loadTask);
	}

	private void refresh() {
		// get previously selected threads
		final Set<H> selectedThreads = new HashSet<H>(getSelectedThreads());

		// get list
		ObservableList<H> obsThreads = FXCollections.observableArrayList(threads);

		// filter
		obsThreads = FXCollections.observableArrayList(obsThreads.filtered(nameFilter));

		// sort
		Collections.sort(obsThreads, sortOrder.getComparator());

		// display
		refreshing = true;
		getItems().setAll(threads);

		// restore selection
		getSelectionModel().clearSelection();
		if (!getItems().isEmpty() && !selectedThreads.isEmpty()) {
			LOGGER.debug("selected threads", threads);
			final int[] indices = new int[selectedThreads.size()];
			Arrays.fill(indices, -1);
			int itemIndex = 0, arrayIndex = 0;
			for(final H t: getItems()) {
				if (selectedThreads.contains(t) && !t.isUnread()) {
					indices[arrayIndex] = itemIndex;
					arrayIndex++;
				}
				itemIndex++;
			}
			getSelectionModel().selectIndices(indices[0], indices);
		}
		refreshing = false;

		loadHandler.handle(null);
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

	public Set<H> getSelectedThreads() {
		final ObservableList<H> selectedList = getSelectionModel().getSelectedItems();
		Set<H> selectedSet;
		if (selectedList.isEmpty()) {
			selectedSet = new HashSet<H>();
		} else {
			selectedSet = Collections.unmodifiableSet(new LinkedHashSet<H>(selectedList));
		}
		return Collections.unmodifiableSet(selectedSet);
	}

	public void setOnSelectThreads(final EventHandler<ActionEvent> handler) {
		getSelectionModel().selectedItemProperty().addListener((ov, newVal, oldVal) -> {
			if (!refreshing) {
				handler.handle(null);
			}
		});
	}

	public void setOnLoadThreads(final EventHandler<ActionEvent> handler) {
		this.loadHandler = handler;
	}
}
