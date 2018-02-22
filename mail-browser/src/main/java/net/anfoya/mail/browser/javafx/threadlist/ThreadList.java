package net.anfoya.mail.browser.javafx.threadlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.model.SimpleThread.SortField;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.model.SpecialTag;

public class ThreadList<T extends Tag, H extends Thread> extends ListView<H> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadList.class);

	private final MailService<?, T, H, ?, ?> mailService;

	private final T unread;

	private final AtomicLong loadTaskId;
	private Task<Set<H>> loadTask;

	private final Set<String> selectedThreadIds;
	private final AtomicInteger selectedIndex;
	
	private Set<T> includes;
	private Set<T> excludes;
	private SortField sortOrder;
	private String pattern;
	private int page;

	private Runnable loadCallback;
	private Runnable updateCallback;

	private Object refreshing;
	private boolean firstLoad;

	private boolean isNewList;
	private boolean isUnreadList;

	public ThreadList(final MailService<?, T, H, ?, ?> mailService) {
        getStyleClass().add("thread-list");
        setPlaceholder(new Label("empty"));
		this.mailService = mailService;

		unread = mailService.getSpecialTag(SpecialTag.UNREAD);
		loadTaskId = new AtomicLong();

		includes = new LinkedHashSet<T>();
		excludes = new LinkedHashSet<T>();
		sortOrder = SortField.DATE;
		pattern = "";

		refreshing = new Object();
		firstLoad = true;

		selectedThreadIds = new HashSet<>();
		selectedIndex = new AtomicInteger(-1);
		
		setCellFactory(param -> new ThreadListCell<H>());

		setOnKeyPressed(e -> handleKey(e));

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		getItems().addListener((final Change<? extends H> c) -> {
			synchronized(refreshing) {
				setFocusTraversable(!getItems().isEmpty());
				restoreSelection();
			}
		});
	}

	private void handleKey(KeyEvent e) {
		if (e.getCode() == KeyCode.BACK_SPACE
				|| e.getCode() == KeyCode.DELETE) {
			archive();
		}
	}

	public void setOnLoad(final Runnable callback) {
		this.loadCallback = callback;
	}

	public void setOnUpdate(final Runnable callback) {
		updateCallback = callback;
	}

	public void sortBy(final SortField order) {
		sortOrder = order;
		load();
	}

	public void loadPage(final int page) {
		this.page = page;
		load();
	}

	public void load(final Set<T> includes, final Set<T> excludes, final String pattern) {		
		isNewList = !includes.equals(this.includes) || !excludes.equals(this.excludes) || !pattern.equals(this.pattern);
		isUnreadList = includes.size() == 1 && includes.contains(unread);

		if (isNewList) {
			page = 1;
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
		final List<H> selectedList;
		synchronized(refreshing) {
			selectedList = new ArrayList<>(getSelectionModel().getSelectedItems());
			LOGGER.debug("threads selected before update: {}", getSelectionModel().getSelectedItems());
		}

		return selectedList.isEmpty()
				? Collections.emptySet()
				: Collections.unmodifiableSet(new LinkedHashSet<H>(selectedList));
	}

	public void setOnSelect(final Runnable callback) {
		getSelectionModel().selectedItemProperty().addListener((ov, n, o) -> {
			synchronized(refreshing) {
				callback.run();
			}
		});
	}

	private synchronized void load() {
		final long taskId = loadTaskId.incrementAndGet();
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
			if (taskId == loadTaskId.get()) {
				refresh(loadTask.getValue());
			}
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, "load thread list", loadTask);
	}

	private void refresh(final Set<H> threads) {
		// save current selected thread list (or selected index in case GmailMoreThreads is selected)
		ObservableList<Integer> selected = getSelectionModel().getSelectedIndices();
		selectedIndex.set(selected.size() == 1? selected.get(0): -1);
		selectedThreadIds.clear();
		selected.parallelStream().forEach(i -> selectedThreadIds.add(getItems().get(i).getId()));

		// get list
		final List<H> sortedThreads = new ArrayList<H>(threads);
		if (isUnreadList && !firstLoad && !isNewList) {
			getItems()
				.parallelStream()
				.filter(t -> !threads.contains(t))
				.forEach(t -> {
					t.getTagIds().remove(unread.getId());
					sortedThreads.add(t);
				});
		}

		// sort
		Collections.sort(sortedThreads, sortOrder.getComparator());
		
		// display
		synchronized(refreshing) {
			getItems().setAll(sortedThreads);
		}

		if (firstLoad) {
			firstLoad  = false;
			if (focusTraversableProperty().get()) {
				// TODO request focus
				requestFocus();
			}
		}

		loadCallback.run();
	}

	private void restoreSelection() {
		LOGGER.debug("previously selected index", selectedIndex);
		LOGGER.debug("previously selected threads {}", selectedThreadIds);

		if (getItems().isEmpty()) {
			getSelectionModel().clearSelection();
			return;
		}

		if (selectedThreadIds.size() == 1 && selectedThreadIds.iterator().next() == GmailMoreThreads.PAGE_TOKEN_ID) {
			restoreMoreThreadSelection();
		} else if (isUnreadList) {
			restoreUnreadSelection();
		} else {
			getSelectedIndices();
		}
		
		LOGGER.debug("selected threads {}", getSelectionModel().getSelectedIndices().toArray());
		
	
	//		
//		final int[] indices = new int[selectedThreadIds.size()];
//		
//		if (!selectedThreadIds.isEmpty()) {
//			Arrays.fill(indices, -1);
//			if (selectedThreadIds.size() == 1 && selectedThreadIds.iterator().next() == GmailMoreThreads.PAGE_TOKEN_ID) {
//				// user opted to see more threads, select first of the added set
//				indices[0] = selectedIndex.get();
//				scrollTo(selectedIndex.get());
//			} else {
//				// find previously selected thread in the new thread list
//				int itemIndex = 0, arrayIndex = 0;
//				for (final H t: getItems()) {
//					if (selectedThreadIds.contains(t.getId())
//							&& isUnreadList
//							|| !isUnreadList && !t.isUnread()) {
//						getSelectionModel().select(t);
//						indices[arrayIndex] = itemIndex;
//						arrayIndex++;
//					}
//					itemIndex++;
//				}
//			}
//		}
//		if (!isUnreadList && !isNewList && getSelectionModel().isEmpty() && selectedIndex.get() != -1) {
//			// select the closest to previous selection
//			int before = -1, after = -1, index = 0;
//			for(final H t: getItems()) {
//				if (!(t instanceof GmailMoreThreads) && !t.isUnread()) { //TODO: put MoreThread in the API
//					if (index < selectedIndex.get()) {
//						before = index;
//					} else {
//						after = index;
//						break;
//					}
//				}
//				index++;
//			}
//			if (after != -1) {
//				indices[0] = after;
//			} else if (before != -1) {
//				indices[0] = before;
//			}
//		}
//		if (!isUnreadList && getSelectionModel().isEmpty()) {
//			// select the first unread
//			for(final H t: getItems()) {
//				if (!t.isUnread() && !(t instanceof GmailMoreThreads)) { //TODO: put MoreThreads in the API
//					getSelectionModel().select(t);
//					break;
//				}
//			}
//		}
//		
//		if (indices[0] != -1) {
////			getSelectionModel().selectIndices(indices[0], Arrays.copyOfRange(indices, Math.min(indices.length-1, 1), Math.max(indices.length, 0)));
//		}
	}

	private void restoreMoreThreadSelection() {
		getSelectionModel().clearAndSelect(selectedIndex.get());
		scrollTo(selectedIndex.get());
	}

	private int[] getSelectedIndices() {
		return null;
	}

	private void restoreUnreadSelection() {		
		if (selectedThreadIds.isEmpty()) {
			getSelectionModel().clearSelection();
		}

		if (selectedThreadIds.size() == 1) {
			getItems()
				.parallelStream()
				.filter(t -> t.getId() == selectedThreadIds.iterator().next())
				.findFirst()
				.ifPresent(t -> getSelectionModel().clearAndSelect(getItems().indexOf(t)));
		}

		if (selectedThreadIds.size() > 1) {
			getSelectionModel().clearSelection();
			getItems()
				.parallelStream()
				.filter(t -> selectedThreadIds.contains(t.getId()))
				.forEach(t -> getSelectionModel().select(t));
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
		task.setOnSucceeded(e -> updateCallback.run());
		task.setOnFailed(e -> LOGGER.error("archive threads {}", threads));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "archive threads", task);
	}
}
