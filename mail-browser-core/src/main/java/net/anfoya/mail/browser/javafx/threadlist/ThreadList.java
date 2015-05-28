package net.anfoya.mail.browser.javafx.threadlist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.message.MessageComposer;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.model.SimpleThread.SortOrder;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadList<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread, M extends SimpleMessage>
		extends ListView<H> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadList.class);

	private final MailService<S, T, H, M> mailService;

	private boolean refreshing;
	private Set<H> threads;

	private Set<T> includes;
	private Set<T> excludes;
	private SortOrder sortOrder;
	private String namePattern;
	private int pageMax;

	private EventHandler<ActionEvent> loadHandler;

	private long loadTaskId;
	private Task<Set<H>> loadTask;

	private EventHandler<ActionEvent> updateHandler;

	public ThreadList(final MailService<S, T, H, M> mailService) {
		this.mailService = mailService;

		this.refreshing = false;
		this.threads = new LinkedHashSet<H>();

		this.includes = new LinkedHashSet<T>();
		this.excludes = new LinkedHashSet<T>();
		this.sortOrder = SortOrder.DATE;
		this.namePattern = "";

		setCellFactory(param -> new ThreadListCell<H>());
		setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.BACK_SPACE
					|| event.getCode() == KeyCode.DELETE) {
				archive();
			}
		});
		setOnMousePressed(event -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
				final Set<H> threads = getSelectedThreads();
				if (threads.size() > 0 && threads.iterator().next().getMessageIds().size() > 0) {
					final String id = threads.iterator().next().getLastMessageId();
					new MessageComposer<M>(mailService, updateHandler).editOrReply(id);
				}
			}
		});

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	private void archive() {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.archive(getSelectedThreads());
				return null;
			}
		};
		task.setOnFailed(event -> {/* TODO Auto-generated catch block */
		});
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	public void refreshWithPattern(final String pattern) {
		namePattern = pattern.toLowerCase();
		load();
	}

	public void refreshWithOrder(final SortOrder order) {
		sortOrder = order;
		load();
	}

	public void refreshWithPage(final int page) {
		pageMax = page;
		load();
	}

	public void refresh(final Set<T> includes, final Set<T> excludes) {
		this.includes = includes;
		this.excludes = excludes;
		this.pageMax = 1;
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
				LOGGER.debug(
						"loading for includes {}, excludes {}, pattern: {}, pageMax: {}",
						includes, excludes, namePattern, pageMax);
				return mailService.findThreads(includes, excludes, namePattern,
						pageMax);
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
		final int selectedIndex = getSelectionModel().getSelectedIndex();

		// get list
		final List<H> sortedThreads = new ArrayList<H>(threads);

		// sort
		Collections.sort(sortedThreads, sortOrder.getComparator());

		// display
		refreshing = true;
		getItems().setAll(sortedThreads);

		// restore selection
		getSelectionModel().clearSelection();
		if (!getItems().isEmpty() && !selectedThreads.isEmpty()) {
			LOGGER.debug("selected threads {}", threads);
			final int[] indices = new int[selectedThreads.size()];
			Arrays.fill(indices, -1);
			if (selectedThreads.size() == 1
					&& selectedThreads.iterator().next() instanceof GmailMoreThreads) {
				indices[0] = selectedIndex;
				scrollTo(indices[0]);
			} else {
				int itemIndex = 0, arrayIndex = 0;
				for (final H t : getItems()) {
					if (selectedThreads.contains(t) && !t.isUnread()) {
						indices[arrayIndex] = itemIndex;
						arrayIndex++;
					}
					itemIndex++;
				}
			}
			getSelectionModel().selectIndices(indices[0], indices);
		}
		refreshing = false;

		loadHandler.handle(null);
	}

	public Set<T> getThreadsTags() {
		// return all tags available from all threads
		final Set<T> tags = new LinkedHashSet<T>();
		for (final H thread : getItems()) {
			for (final String id : thread.getTagIds()) {
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
		final ObservableList<H> selectedList = getSelectionModel()
				.getSelectedItems();
		Set<H> selectedSet;
		if (selectedList.isEmpty()) {
			selectedSet = new HashSet<H>();
		} else {
			selectedSet = Collections.unmodifiableSet(new LinkedHashSet<H>(
					selectedList));
		}
		return Collections.unmodifiableSet(selectedSet);
	}

	public void setOnSelectThreads(final EventHandler<ActionEvent> handler) {
		getSelectionModel().selectedItemProperty().addListener(
				(ov, newVal, oldVal) -> {
					if (!refreshing) {
						handler.handle(null);
					}
				});
	}

	public void setOnLoad(final EventHandler<ActionEvent> handler) {
		this.loadHandler = handler;
	}

	public void setOnUpdate(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}
}
