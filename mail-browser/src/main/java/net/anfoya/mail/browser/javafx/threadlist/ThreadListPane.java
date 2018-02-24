package net.anfoya.mail.browser.javafx.threadlist;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.VoidCallable;
import net.anfoya.java.util.VoidCallback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.javafx.scene.animation.DelayTimeline;
import net.anfoya.javafx.scene.control.ResetTextField;
import net.anfoya.javafx.scene.dnd.DndHelper;
import net.anfoya.mail.browser.javafx.BrowserToolBar;
import net.anfoya.mail.browser.javafx.MailBrowser.Mode;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.model.SimpleThread.SortField;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.javafx.scene.dnd.ExtItemDropPane;
import net.anfoya.tag.model.SpecialTag;

public class ThreadListPane<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadListPane.class);

	public static final DataFormat DND_THREADS_DATA_FORMAT = new DataFormat("DND_THREADS_DATA_FORMAT");

	private final MailService<S, T, H, M, C> mailService;
	private final UndoService undoService;
	private final Settings settings;

	private final ThreadList<T, H> threadList;
	private final ResetTextField patternField;

	private final BrowserToolBar<S, T, M, C> toolBar;
	private final ThreadListDropPane threadListDropPane;
	
	private final T inbox;

	private DelayTimeline patternDelay;

	private S currentSection;

	private VoidCallback<Set<H>> archive;
	private VoidCallback<Set<H>> reply;
	private VoidCallback<Set<H>> replyAll;
	private VoidCallback<Set<H>> forward;
	private VoidCallback<Set<H>> toggleFlag;
	private VoidCallback<Set<H>> trash;
	private VoidCallback<Set<H>> toggleSpam;

	private VoidCallback<Set<H>> selectCallback;
	private VoidCallback<Set<H>> openCallback;
	
	private VoidCallback<T> tagUpdateCallback;

	public ThreadListPane(final MailService<S, T, H, M, C> mailService
			, final UndoService undoService
			, final Settings settings) throws MailException {
		this.mailService = mailService;
		this.undoService = undoService;
		this.settings = settings;

		inbox = mailService.getSpecialTag(SpecialTag.INBOX);

		patternField = new ResetTextField();
		patternField.setPromptText("thread search");

		threadList = new ThreadList<T, H>(mailService);
		threadList.setOnMouseClicked(e -> threadListClicked(e));
		threadList.setOnDragDetected(e -> {
			final ClipboardContent content = new ClipboardContent();
			content.put(ExtItemDropPane.ADD_TAG_DATA_FORMAT, "");
			content.put(DND_THREADS_DATA_FORMAT, "");

			int count = 0;
			final StackPane stackPane = new StackPane();
			final ThreadListCell<H> cell = new ThreadListCell<H>();
			for (final H t : getSelectedThreads()) {
				final Pane grid = cell.buildGridPane(t);
				grid.getStyleClass().add("thread-list-cell-dnd");
				grid.setTranslateX(4 * count);
				grid.setTranslateY(8 * count);
				stackPane.getChildren().add(grid);
				count++;
			}
			final Image image = new DndHelper(getScene().getStylesheets()).toImage(stackPane);

			final Dragboard db = threadList.startDragAndDrop(TransferMode.ANY);
			db.setContent(content);
			db.setDragView(image, image.getWidth() / 2, image.getHeight() / 2);
		});
		threadList.setOnDragDone(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasContent(Tag.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) db.getContent(Tag.TAG_DATA_FORMAT);
				final Set<H> selected = getSelectedThreads();
				addTagForThreads(tag, selected, "add " + tag.getName()
						, () -> removeTagForThreads(tag, selected, "remove" + tag.getName(), null));

			} else if (db.hasContent(ExtItemDropPane.TAG_NAME_DATA_FORMAT)) {
				final String name = (String) db.getContent(ExtItemDropPane.TAG_NAME_DATA_FORMAT);
				createTagForThreads(name, getSelectedThreads());
			}
			e.consume();
		});

		final StackPane centerPane = new StackPane(threadList);
		centerPane.setAlignment(Pos.BOTTOM_CENTER);
		setCenter(centerPane);

		centerPane.getChildren().add(new UndoPane(undoService));
		centerPane.getChildren().add(new DisconnectedPane(mailService));

		final VBox topBox = new VBox();
		setTop(topBox);

		toolBar = new BrowserToolBar<S, T, M, C>(mailService, undoService, settings);
		toolBar.setVisibles(true, false, false);

		final HBox firstLineBox = new HBox(patternField, toolBar);
		firstLineBox.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(patternField, Priority.ALWAYS);
		topBox.getChildren().add(firstLineBox);

		final ThreadToolBar toolbar = new ThreadToolBar();
		toolbar.setFocusTraversable(false);
		toolbar.setPadding(new Insets(0));
		toolbar.setOnReply(e -> reply.call(getSelectedThreads()));
		toolbar.setOnReplyAll(e -> replyAll.call(getSelectedThreads()));
		toolbar.setOnForward(e -> forward.call(getSelectedThreads()));
		toolbar.setOnToggleFlag(e -> toggleFlag.call(getSelectedThreads()));
		toolbar.setOnArchive(e -> archive.call(getSelectedThreads()));
		toolbar.setOnTrash(e -> trash.call(getSelectedThreads()));
		toolbar.setOnSpam(e -> toggleSpam.call(getSelectedThreads()));

		final BooleanProperty showToolbar = settings.showToolbar();
		if (showToolbar.get()) {
			topBox.getChildren().add(toolbar);
		}
		showToolbar.addListener((ov, o, n) -> {
			if (n) {
				topBox.getChildren().add(toolbar);
			} else {
				topBox.getChildren().remove(toolbar);
			}
		});

		threadListDropPane = new ThreadListDropPane();
		threadListDropPane.prefWidthProperty().bind(centerPane.widthProperty());
		threadListDropPane.setOnArchive(e -> archive.call(getSelectedThreads()));
		threadListDropPane.setOnForward(e -> forward.call(getSelectedThreads()));
		threadListDropPane.setOnReply(all -> {
			if (all) {
				reply.call(getSelectedThreads());
			} else {
				replyAll.call(getSelectedThreads());
			}
		});

		centerPane.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)
					&& !centerPane.getChildren().contains(threadListDropPane)) {
				centerPane.getChildren().add(threadListDropPane);
			}

		});
		centerPane.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)
					&& centerPane.getChildren().contains(threadListDropPane)) {
				centerPane.getChildren().remove(threadListDropPane);
			}
		});

		final Label sortLabel = new Label(SortField.DATE.toString().toLowerCase());
		sortLabel.setOnMouseClicked(e -> {
			final SortField field;
			if (sortLabel.getText().equalsIgnoreCase(SortField.DATE.toString())) {
				field = SortField.SENDER;
			} else {
				field = SortField.DATE;
			}
			sortLabel.setText(field.toString().toLowerCase());
			threadList.sortBy(field);
		});

		final FlowPane sortPane = new FlowPane(new Label("sorted by "), sortLabel);
		sortPane.setAlignment(Pos.CENTER);
		setBottom(sortPane);
		setMargin(sortPane, new Insets(5));
	}

	private void threadListClicked(MouseEvent e) {
		final Set<H> threads = getSelectedThreads();
		if (threads.isEmpty()) {
			return;
		}

		e.consume();
		
		if (e.getClickCount() == 1) {
			selectCallback.call(threads);
		} else if (e.getClickCount() == 1) {
			openCallback.call(threads);
		}
	}

	public void setOnArchive(VoidCallback<Set<H>> callback) {
		this.archive = callback;
	}

	public void setOnReply(VoidCallback<Set<H>> callback) {
		this.reply = callback;
	}

	public void setOnReplyAll(VoidCallback<Set<H>> callback) {
		this.replyAll = callback;
	}

	public void setOnForward(VoidCallback<Set<H>> callback) {
		this.forward = callback;
	}

	public void setOnToggleFlag(VoidCallback<Set<H>> callback) {
		this.toggleFlag = callback;
	}

	public void setOnTrash(VoidCallback<Set<H>> callback) {
		this.trash = callback;
	}

	public void setOnToggleSpam(VoidCallback<Set<H>> callback) {
		this.toggleSpam = callback;
	}

	private Void addTagForThreads(final T tag, final Set<H> threads, final String desc, final VoidCallable undo) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(tag, threads);
				if (settings.archiveOnDrop().get() && !tag.isSystem()) {
					mailService.archive(threads);
				}
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		task.setOnSucceeded(e -> {
			undoService.set(undo, desc);
			tagUpdateCallback.call(tag);
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
		return null;
	}

	private Void removeTagForThreads(final T tag, final Set<H> threads, final String desc, final VoidCallable undo) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.removeTagForThreads(tag, threads);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		task.setOnSucceeded(e -> {
			undoService.set(undo, desc);
			tagUpdateCallback.call(tag);
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
		return null;
	}

	private void createTagForThreads(final String name, final Set<H> threads) {
		final Iterator<H> iterator = threads.iterator();
		final boolean hasInbox = iterator.hasNext() && iterator.next().getTagIds().contains(inbox.getId());
		final String desc = "add " + name;

		final Task<T> task = new Task<T>() {
			@Override
			protected T call() throws Exception {
				final T tag = mailService.addTag(name);
				if (currentSection != null && !currentSection.isSystem()) {
					mailService.moveToSection(tag, currentSection);
				}
				mailService.addTagForThreads(tag, threads);
				if (settings.archiveOnDrop().get() && hasInbox) {
					mailService.archive(threads);
				}
				return tag;
			}
		};
		task.setOnSucceeded(e -> {
			final T tag = task.getValue();
			undoService.set(() -> {
				mailService.remove(tag);
				if (hasInbox && settings.archiveOnDrop().get()) {
					mailService.addTagForThreads(inbox, threads);
				}
			}, desc);
			tagUpdateCallback.call(tag);
		});
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
	}

	public String getNamePattern() {
		return patternField.getText();
	}

	public void refreshWithTags(final Set<T> includes, final Set<T> excludes) {
		threadList.load(includes, excludes, patternField.getText());
	}

	public void refreshWithPage(final int page) {
		threadList.loadPage(page);
	}

	public int getThreadCount() {
		return threadList.getItems().size();
	}

	public Set<T> getMoviesTags() {
		return threadList.getThreadsTags();
	}

	public Set<H> getSelectedThreads() {
		return threadList.getSelectedThreads();
	}

	public ObservableList<H> getItems() {
		return threadList.getItems();
	}

	public void setOnView(final VoidCallback<Set<H>> callback) {
		this.selectCallback = callback;
	}

	public void setOnOpen(final VoidCallback<Set<H>> callback) {
		this.openCallback = callback;
	}

	public void setOnOpen(final Runnable callback) {
		threadList.setOnSelect(callback);
	}

	public void setOnLoad(final Runnable callback) {
		threadList.setOnLoad(callback);
	}

	public Set<T> getThreadsTags() {
		final Set<T> tags = new LinkedHashSet<T>();
		for (final H t : getItems()) {
			for (final String id : t.getTagIds()) {
				try {
					tags.add(mailService.getTag(id));
				} catch (final MailException e) {
					LOGGER.error("get tag {}", id, e);
				}
			}
		}
		return tags;
	}

	public void setOnUpdatePattern(final Runnable callback) {
		patternField.textProperty().addListener((ov, o, n) -> {
			if (patternDelay == null) {
				patternDelay = new DelayTimeline(Duration.millis(500), e -> callback.run());
			}
			patternDelay.playFromStart();
		});
	}

	public void setOnTagUpdate(final VoidCallback<T> callback) {
		tagUpdateCallback = callback;
	}

	public void setCurrentSection(final S section) {
		currentSection = section;
	}

	public void setMode(Mode mode) {
		if (mode == Mode.FULL) {
			toolBar.setVisibles(true, false, false);
		} else {
			toolBar.setVisibles(true, true, true);
		}
	}
}
