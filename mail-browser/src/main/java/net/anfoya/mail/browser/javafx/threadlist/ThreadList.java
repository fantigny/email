package net.anfoya.mail.browser.javafx.threadlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Animation.Status;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import net.anfoya.java.util.VoidCallback;
import net.anfoya.javafx.scene.animation.DelayTimeline;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.model.SimpleThread.SortField;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

public class ThreadList<T extends Tag, H extends Thread> extends ListView<H> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadList.class);

	private final AtomicInteger selectedIndex;
	private final Set<String> selectedIds;
	private final Set<H> selectedThreads;

	private final AtomicBoolean unread;
	private boolean firstLoad;
	private SortField sortOrder;

	private Runnable loadCallback;
	private VoidCallback<Set<H>> archiveCallback;
	private VoidCallback<Set<H>> selectCallback;

	private DelayTimeline emptySelectDelay;

	public ThreadList() {
        getStyleClass().add("thread-list");
        setPlaceholder(new Label("empty"));

		selectedThreads = Collections.synchronizedSet(new HashSet<>());

		sortOrder = SortField.DATE;

		firstLoad = true;

		selectedIds = new HashSet<>();
		selectedIndex = new AtomicInteger(-1);
		unread = new AtomicBoolean();

		setCellFactory(param -> new ThreadListCell<>());

		setOnKeyPressed(e -> handleKey(e));

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		getSelectionModel().getSelectedItems().addListener((Change<? extends H> c) -> {
			final Set<H> selectedThreads = c.getList()
					.stream()
					.collect(Collectors.toSet());
			if (emptySelectDelay != null && emptySelectDelay.getStatus() == Status.RUNNING) { //TODO reduce flickering of messages -- to remove
				emptySelectDelay.stop();
			}
			emptySelectDelay = new DelayTimeline(Duration.millis(selectedThreads.isEmpty()? 500: 1), e -> {
				synchronized (selectedThreads) {
					this.selectedThreads.clear();
					this.selectedThreads.addAll(selectedThreads);
				}
				selectCallback.call(getSelectedThreads());
			});
			emptySelectDelay.playFromStart();
		});

		getItems().addListener((final Change<? extends H> c) -> {
			LOGGER.debug("updated list size {}", c.getList().size());
			setFocusTraversable(!c.getList().isEmpty());
			selectThread();
		});
	}

	private void handleKey(KeyEvent e) {
		switch(e.getCode()) {
		case BACK_SPACE: case DELETE:
			archiveCallback.call(getSelectedThreads());
			break;
		default:
			break;
		}
	}

	public void setOnLoad(final Runnable callback) {
		this.loadCallback = callback;
	}

	public void setOnArchive(final VoidCallback<Set<H>> callback) {
		archiveCallback = callback;
	}

	public void sortBy(final SortField order) {
		sortOrder = order;
		setAll(new HashSet<H>(getItems()), unread.get());
	}

	public Set<H> getSelectedThreads() {
		return new HashSet<>(selectedThreads);
	}

	public void setOnSelect(final Runnable callback) {
		getSelectionModel().selectedItemProperty().addListener((ov, n, o) -> callback.run());
	}

	public void setAll(final Set<H> threads, boolean unread) {
		// allow specific behavior for unread list
		this.unread.set(unread);

		// save current selected thread list and selected index (in case of GmailMoreThreads)
		selectedIndex.set(getSelectionModel().getSelectedIndex());
		selectedIds.clear();
		selectedIds.addAll(getSelectionModel().getSelectedItems()
				.stream()
				.map(t -> t.getId())
				.collect(Collectors.toSet()));

		// sort
		final List<H> sortedThreads = new ArrayList<>(threads);
		Collections.sort(sortedThreads, sortOrder.getComparator());

		// display
		getItems().setAll(sortedThreads);

		// request focus on first load
		if (firstLoad) {
			firstLoad  = false;
			if (focusTraversableProperty().get()) {
				requestFocus();
			}
		}

		loadCallback.run();
	}

	private void selectThread() {
		LOGGER.debug("previously selected index ({})", selectedIndex);
		LOGGER.debug("previously selected thread ids {}", selectedIds);

		if (getItems().isEmpty()) {
			getSelectionModel().clearSelection();
			return;
		}

		if (!selectedIds.isEmpty() && selectedIds.iterator().next() == GmailMoreThreads.PAGE_TOKEN_ID) {
			// user clicked "more thread", new selection is the starts of the new set
			selectFirstOfAddedSet();
		} else if (unread.get()) {
			restoreUnreadSelection();
		} else {
			restoreSelection();
		}

		LOGGER.debug("restored selection with list of index {}", getSelectionModel().getSelectedIndices());
	}

	private void restoreUnreadSelection() {
		final boolean wasSingleSelection = selectedIds.size() == 1;
		final boolean isMultipleSelection = !wasSingleSelection && getItems()
				.stream()
				.filter(t -> selectedIds.contains(t.getId()))
				.mapToInt(t -> getItems().indexOf(t))
				.filter(i -> i != -1)
				.count() > 1;

		// try to select the same item(s) as before
		final int[] indices = getItems()
				.stream()
				.filter(t -> selectedIds.contains(t.getId()) && (!t.isUnread() || isMultipleSelection))
				.mapToInt(t -> getItems().indexOf(t))
				.filter(i -> i != -1)
				.toArray();

		getSelectionModel().selectIndices(-1, indices);
	}

	private void restoreSelection() {
		final int index = Math.min(Math.max(0, selectedIndex.get()), getItems().size());
		final boolean wasSingleSelection = selectedIds.size() == 1;
		final boolean isMultipleSelection = !wasSingleSelection && getItems()
				.stream()
				.filter(t -> selectedIds.contains(t.getId()))
				.mapToInt(t -> getItems().indexOf(t))
				.filter(i -> i != -1)
				.count() > 1;

		// try to select the same item(s) as before
		int[] indices = getItems()
				.stream()
				.filter(t -> selectedIds.contains(t.getId()) && (!t.isUnread() || isMultipleSelection))
				.mapToInt(t -> getItems().indexOf(t))
				.filter(i -> i != -1)
				.toArray();

		if (indices.length == 0) {
			// try to find the closest following unread thread
			indices = getItems().subList(index, getItems().size())
					.stream()
					.mapToInt(t -> getItems().indexOf(t))
					.filter(i -> i != -1)
					.toArray();
			indices = indices.length < 2? indices: new int[] { indices[0] };
		}

		if (indices.length == 0) {
			// try to find the closest preceding unread thread
			indices = getItems().subList(0, index)
					.stream()
					.sorted(sortOrder.getComparator().reversed())
					.mapToInt(t -> getItems().indexOf(t))
					.filter(i -> i != -1)
					.toArray();
			indices = indices.length < 2? indices: new int[] { indices[0] };
		}

		getSelectionModel().selectIndices(-1, indices);
	}

	private void selectFirstOfAddedSet() {
		getSelectionModel().selectIndices(selectedIndex.get());
		scrollTo(selectedIndex.get());
	}

	public void setOnSelect(VoidCallback<Set<H>> callback) {
		selectCallback = callback;
	}
}
