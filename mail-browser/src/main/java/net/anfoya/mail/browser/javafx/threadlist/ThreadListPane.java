package net.anfoya.mail.browser.javafx.threadlist;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.javafx.scene.animation.DelayTimeline;
import net.anfoya.javafx.scene.control.ResetTextField;
import net.anfoya.javafx.scene.dnd.DndHelper;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.model.SimpleThread.SortOrder;
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

	private final ThreadList<S, T, H, M, C> threadList;
	private final ResetTextField patternField;

	private final T inbox;
	private final T spam;
	private final T trash;
	private final T flagged;

	private DelayTimeline patternDelay;

	private final ThreadListDropPane threadListDropPane;

	private EventHandler<ActionEvent> updateHandler;
	private S currentSection;

	public ThreadListPane(final MailService<S, T, H, M, C> mailService
			, final UndoService undoService
			, final Settings settings) throws MailException {
		this.mailService = mailService;
		this.undoService = undoService;
		this.settings = settings;

		inbox = mailService.getSpecialTag(SpecialTag.INBOX);
		spam = mailService.getSpecialTag(SpecialTag.SPAM);
		trash = mailService.getSpecialTag(SpecialTag.TRASH);
		flagged = mailService.getSpecialTag(SpecialTag.FLAGGED);

		patternField = new ResetTextField();
		patternField.setPromptText("thread search");

		threadList = new ThreadList<S, T, H, M, C>(mailService, settings);
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

		final Button newButton = new Button();
		newButton.setFocusTraversable(false);
		newButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/new.png"))));
		newButton.setTooltip(new Tooltip("new"));
		newButton.setOnAction(event -> {
			try {
				new MailComposer<M, C>(mailService, updateHandler, settings).newMessage("");
			} catch (final Exception e) {
				LOGGER.error("load new message composer", e);
			}
		});

		final VBox topBox = new VBox();
		setTop(topBox);

		final HBox firstLineBox = new HBox(patternField, newButton);
		firstLineBox.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(patternField, Priority.ALWAYS);
		topBox.getChildren().add(firstLineBox);

		final ThreadListToolBar toolbar = new ThreadListToolBar();
		toolbar.setFocusTraversable(false);
		toolbar.setPadding(new Insets(0));
		toolbar.setOnReply(e -> replySelected(false));
		toolbar.setOnReplyAll(e -> replySelected(true));
		toolbar.setOnForward(e -> forwardSelected());
		toolbar.setOnToggleFlag(e -> toggleFlag());
		toolbar.setOnArchive(e -> archiveSelected());
		toolbar.setOnTrash(e -> trashSelected());
		toolbar.setOnSpam(e -> toggleSpam());

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
		threadListDropPane.setOnArchive(e -> archiveSelected());
		threadListDropPane.setOnForward(e -> forwardSelected());
		threadListDropPane.setOnReply(all -> replySelected(all));

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

		final ToggleGroup toggleGroup = new ToggleGroup();

		final RadioButton nameSortButton = new RadioButton("sender");
		nameSortButton.setFocusTraversable(false);
		nameSortButton.setToggleGroup(toggleGroup);

		final RadioButton dateSortButton = new RadioButton("date");
		dateSortButton.setFocusTraversable(false);
		dateSortButton.setToggleGroup(toggleGroup);

		toggleGroup.selectedToggleProperty().addListener((ChangeListener<Toggle>) (ov, oldVal, newVal) -> threadList
				.setOrder(nameSortButton.isSelected() ? SortOrder.SENDER : SortOrder.DATE));
		dateSortButton.setSelected(true);

		final HBox sortBox = new HBox(5, new Label("by"), dateSortButton, nameSortButton);
		sortBox.setAlignment(Pos.CENTER);
		setBottom(sortBox);
		setMargin(sortBox, new Insets(5));
	}

	private void forwardSelected() {
		try {
			for (final H t : threadList.getSelectedThreads()) {
				final M message = mailService.getMessage(t.getLastMessageId());
				new MailComposer<M, C>(mailService, updateHandler, settings)
					.forward(message);
			}
		} catch (final Exception e) {
			LOGGER.error("load forward composer", e);
		}
	}

	private Void replySelected(final boolean all) {
		try {
			for (final H t : threadList.getSelectedThreads()) {
				final M message = mailService.getMessage(t.getLastMessageId());
				new MailComposer<M, C>(mailService, updateHandler, settings)
					.reply(message, all);
			}
		} catch (final Exception e) {
			LOGGER.error("load reply{} composer", all ? " all" : "", e);
		}
		return null;
	}

	private void trashSelected() {
		final Set<H> threads = threadList.getSelectedThreads();
		final Iterator<H> iterator = threads.iterator();
		final boolean hasInbox = iterator.hasNext() && iterator.next().getTagIds().contains(inbox.getId());

		new AudioClip(Settings.MP3_TRASH).play();

		final String description = "trash";
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.trash(threads);
				return null;
			}
		};
		task.setOnSucceeded(e -> {
			undoService.set(() -> {
				mailService.removeTagForThreads(trash, threads);
				if (hasInbox) {
					mailService.addTagForThreads(inbox, threads);
				}
				return null;
			}, description);
			updateHandler.handle(null);
		});
		task.setOnFailed(e -> LOGGER.error(description, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, description, task);
	}

	private void archiveSelected() {
		final Set<H> threads = threadList.getSelectedThreads();
		final String description = "archive";
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.archive(threads);
				return null;
			}
		};
		task.setOnSucceeded(e -> {
			undoService.set(() -> {
				mailService.addTagForThreads(inbox, threads);
				return null;
			}, description);
			updateHandler.handle(null);
		});
		task.setOnFailed(e -> LOGGER.error(description, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, description, task);
	}

	private void toggleFlag() {
		final Set<H> threads = threadList.getSelectedThreads();
		if (threads.isEmpty()) {
			return;
		}
		try {
			if (threads.iterator().next().isFlagged()) {
				removeTagForThreads(flagged, threads, "unflag"
						, () -> addTagForThreads(flagged, threads, "flag", null));
			} else {
				addTagForThreads(flagged, threads, "flag"
						, () -> removeTagForThreads(flagged, threads, "unflag", null));
			}
		} catch (final Exception e) {
			LOGGER.error("toggle flag", e);
		}
	}

	private void toggleSpam() {
		final Set<H> threads = threadList.getSelectedThreads();
		if (threads.isEmpty()) {
			return;
		}
		try {
			if (threads.iterator().next().getTagIds().contains(spam.getId())) {
				removeTagForThreads(spam, threads, "not spam", null);
				addTagForThreads(inbox, threads, "not spam", () -> addTagForThreads(spam, threads, "not spam", null));
			} else {
				addTagForThreads(spam, threads, "spam", () -> {
					removeTagForThreads(spam, threads, "spam", null);
					addTagForThreads(inbox, threads, "spam", null);
					return null;
				});
			}
		} catch (final Exception e) {
			LOGGER.error("toggle flag", e);
		}
	}

	private Void addTagForThreads(final T tag, final Set<H> threads, final String desc, final Callable<Object> undo) {
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
			updateHandler.handle(null);
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
		return null;
	}

	private Void removeTagForThreads(final T tag, final Set<H> threads, final String desc, final Callable<Object> undo) {
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
			updateHandler.handle(null);
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
			undoService.set(() -> {
				mailService.remove(task.getValue());
				if (settings.archiveOnDrop().get() && hasInbox) {
					mailService.addTagForThreads(inbox, threads);
				}
				return null;
			}, desc);
			updateHandler.handle(null);
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

	public void setOnSelectThread(final EventHandler<ActionEvent> handler) {
		threadList.setOnSelectThreads(handler);
	}

	public void setOnLoadThreadList(final EventHandler<ActionEvent> handler) {
		threadList.setOnLoad(handler);
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

	public void setOnUpdatePattern(final EventHandler<ActionEvent> handler) {
		patternField.textProperty().addListener((ov, oldVal, newVal) -> {
			if (patternDelay != null) {
				patternDelay.stop();
			}
			patternDelay = new DelayTimeline(Duration.millis(500), e -> handler.handle(null));
			patternDelay.play();
		});
	}

	public void setOnUpdateThread(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
		threadList.setOnUpdate(handler);
	}

	public void setCurrentSection(final S section) {
		currentSection = section;
	}
}
