package net.anfoya.mail.browser.javafx.threadlist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.browser.javafx.message.MailReader;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.model.SimpleThread.SortOrder;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

public class ThreadList<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends ListView<H> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadList.class);

	private final MailService<S, T, H, M, C> mailService;
	private final UndoService undoService;
	private final Settings settings;

	private Set<T> includes;
	private Set<T> excludes;
	private SortOrder sortOrder;
	private String pattern;
	private int page;

	private long loadTaskId;
	private Task<Set<H>> loadTask;

	private boolean refreshing;
	private boolean resetSelection;

	private EventHandler<ActionEvent> loadHandler;
	private EventHandler<ActionEvent> updateHandler;

	private boolean firstLoad = true;

	public ThreadList(final MailService<S, T, H, M, C> mailService
			, UndoService undoService
			, final Settings settings) {
        getStyleClass().add("thread-list");
        setPlaceholder(new Label("empty"));
		this.mailService = mailService;
		this.undoService = undoService;
		this.settings = settings;

		includes = new LinkedHashSet<T>();
		excludes = new LinkedHashSet<T>();
		sortOrder = SortOrder.DATE;
		pattern = "";

		refreshing = false;
		resetSelection = true;

		setCellFactory(param -> new ThreadListCell<H>());
		setOnKeyPressed(e -> handleKey(e));
		setOnMousePressed(e -> handleMouse(e));

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		getSelectionModel().selectedIndexProperty().addListener((ov, o, n) -> checkForSelection(o.intValue(), n.intValue()));

		getItems().addListener((final Change<? extends H> c) -> setFocusTraversable(!getItems().isEmpty()));
	}

	private void handleKey(KeyEvent e) {
		if (e.getCode() == KeyCode.BACK_SPACE
				|| e.getCode() == KeyCode.DELETE) {
			archive();
		}
	}

	private void handleMouse(MouseEvent e) {
		if (e.getClickCount() == 2) {
			e.consume();
		} else {
			return;
		}

		final Set<H> threads = getSelectedThreads();
		if (threads.isEmpty()) {
			return;
		}
		final H thread = threads.iterator().next();
		if (e.isPrimaryButtonDown()) {
			new MailReader<S, T, H, M, C>(mailService
					, undoService
					, settings
					, threads.iterator().next()
					, updateHandler).show();
		} else if (e.isSecondaryButtonDown()) {
			new MailComposer<M, C>(mailService, updateHandler, settings)
				.editOrReply(thread.getLastMessageId(), settings.replyAllDblClick().get());
		}
	}

	public void setOnLoad(final EventHandler<ActionEvent> handler) {
		this.loadHandler = handler;
	}

	public void setOnUpdate(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	public void setOrder(final SortOrder order) {
		sortOrder = order;
		load();
	}

	public void loadPage(final int page) {
		this.page = page;
		load();
	}

	public void load(final Set<T> includes, final Set<T> excludes, final String pattern) {
		if (!includes.equals(this.includes)
				|| !excludes.equals(this.excludes)
				|| !pattern.equals(this.pattern)) {
			this.page = 1;
		}
		this.includes = includes;
		this.excludes = excludes;
		this.pattern = pattern;
		load();
	}

	public Set<T> getThreadsTags() {
		// return all tags available from all threads
		final Set<T> tags = new LinkedHashSet<T>();
		for (final H thread : getItems()) {
			for (final String id : thread.getTagIds()) {
				try {
					tags.add(mailService.getTag(id));
				} catch (final MailException e) {
					LOGGER.error("load tag", e);
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

	private synchronized void load() {
		final long taskId = ++loadTaskId;
		if (loadTask != null && loadTask.isRunning()) {
			loadTask.cancel();
		}
		loadTask = new Task<Set<H>>() {
			@Override
			protected Set<H> call() throws InterruptedException, MailException {
				LOGGER.debug("load for includes {}, excludes {}, pattern: {}, pageMax: {}", includes, excludes, pattern, page);
				final Set <H> threads = mailService.findThreads(includes, excludes, pattern, page);
				return threads;
			}
		};
		loadTask.setOnFailed(e -> LOGGER.error("load thread list", e.getSource().getException()));
		loadTask.setOnSucceeded(e -> {
			if (taskId != loadTaskId) {
				return;
			}
			refresh(loadTask.getValue());
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, "load thread list", loadTask);
	}

	private void refresh(final Set<H> threads) {
		// keep previous selection data
		final List<H> oldThreads = new ArrayList<H>(getItems());
		final int oldSelectedIndex = getSelectionModel().getSelectedIndex();
		final Set<H> oldSelectedThreads = new HashSet<H>(getSelectedThreads());

		// get list
		final List<H> sortedThreads = new ArrayList<H>(threads);

		// sort
		Collections.sort(sortedThreads, sortOrder.getComparator());

		// display
		refreshing = true;
		resetSelection = true;
		getItems().setAll(sortedThreads);
		restoreSelection(oldThreads, oldSelectedIndex, oldSelectedThreads);
		refreshing = false;

		if (firstLoad) {
			firstLoad  = false;
			if (focusTraversableProperty().get()) {
				// TODO request focus
				requestFocus();
			}
		}

		loadHandler.handle(null);
	}

	private void restoreSelection(final List<H> oldList, final int oldSelectedIndex, final Set<H> oldSelectedList) {
		getSelectionModel().clearSelection();

		if (getItems().isEmpty()) {
			return;
		}

		if (!oldSelectedList.isEmpty()) {
			LOGGER.debug("selected threads {}", oldList);
			final int[] indices = new int[oldSelectedList.size()];
			Arrays.fill(indices, -1);
			if (oldSelectedList.size() == 1 && oldSelectedList.iterator().next() instanceof GmailMoreThreads) {
				// user wants to see more threads, select first of the "next threads"
				indices[0] = oldSelectedIndex;
				scrollTo(indices[0]);
			} else {
				// find thread(s) previously selected in the new thread list
				int itemIndex = 0, arrayIndex = 0;
				for (final H t: getItems()) {
					if (oldSelectedList.contains(t) && !t.isUnread()) {
						indices[arrayIndex] = itemIndex;
						arrayIndex++;
					}
					itemIndex++;
				}
			}
			if (indices[0] != -1) {
				getSelectionModel().selectIndices(indices[0], indices);
			}
		}
		if (getSelectionModel().isEmpty() && oldSelectedIndex != -1) {
			// select the closest to previous selection
			int before = -1, after = -1, index = 0;
			for(final H t: getItems()) {
				if (!(t instanceof GmailMoreThreads)) { //TODO: put MoreThread in the API
					if (index < oldSelectedIndex) {
						before = index;
					} else {
						after = index;
						break;
					}
				}
				index++;
			}
			if (after != -1 && oldList.contains(getItems().get(after))) {
				getSelectionModel().select(after);
			} else if (before != -1 && oldList.contains(getItems().get(before))) {
				getSelectionModel().select(before);
			}
		}
		if (getSelectionModel().isEmpty()) {
			// select the first unread
			for(final H t: getItems()) {
				if (!t.isUnread() && !(t instanceof GmailMoreThreads)) { //TODO: put MoreThreads in the API
					getSelectionModel().select(t);
					break;
				}
			}
		}
	}

	private void checkForSelection(final int prevIndex, final int newIndex) {
		if (resetSelection) {
			resetSelection = false;
			return;
		}
		if (prevIndex != -1 && newIndex == -1) {
			//TODO remove all that
			new Timer("threadlist-selection-schedule", true).schedule(new TimerTask() {
				@Override
				public void run() {
					Platform.runLater(() -> {
						if (getSelectionModel().selectedItemProperty().isNull().get()
								&& !getItems().get(prevIndex).isUnread()) {
							getSelectionModel().select(prevIndex);
						}
					});
				}
			}, 500);
		}
	}

	private void archive() {
		final Set<H> threads = getSelectedThreads();
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.archive(threads);
				return null;
			}
		};
		task.setOnSucceeded(e -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("archive threads {}", threads));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "archive threads", task);
	}
}
