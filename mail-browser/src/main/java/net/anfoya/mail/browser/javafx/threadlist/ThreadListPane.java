package net.anfoya.mail.browser.javafx.threadlist;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.animation.DelayTimeline;
import net.anfoya.javafx.scene.control.ResetTextField;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.model.SimpleThread.SortOrder;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.SpecialTag;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;

public class ThreadListPane<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadListPane.class);
	private static final ReadOnlyBooleanProperty ARCHIVE_ON_DROP = Settings.getSettings().archiveOnDrop();

	public static final DataFormat DND_THREADS_DATA_FORMAT = new DataFormat("Set<" + Thread.class.getName() + ">");

	private final MailService<S, T, H, M, C> mailService;
	private final ThreadList<S, T, H, M, C> threadList;
	private final ResetTextField patternField;

	private final T inbox;
	private final T spam;
	private final T flagged;

	private DelayTimeline patternDelay;

	private ThreadListDropPane<T, H, M, C> threadListDropPane;

	private EventHandler<ActionEvent> updateHandler;

	public ThreadListPane(final MailService<S, T, H, M, C> mailService) throws MailException {
		this.mailService = mailService;

		inbox = mailService.getSpecialTag(SpecialTag.INBOX);
		spam = mailService.getSpecialTag(SpecialTag.SPAM);
		flagged = mailService.getSpecialTag(SpecialTag.FLAGGED);

		final StackPane stackPane = new StackPane();
		stackPane.setAlignment(Pos.BOTTOM_CENTER);
		setCenter(stackPane);

		final BorderPane threadListPane = new BorderPane();
		stackPane.getChildren().add(threadListPane);

		threadList = new ThreadList<S, T, H, M, C>(mailService);
		threadList.setOnDragDetected(event -> {
			final Set<H> threads = getSelectedThreads();
			if (threads.isEmpty()) {
				return;
			}
	        final ClipboardContent content = new ClipboardContent();
	        content.put(DND_THREADS_DATA_FORMAT, threads);
	        final Dragboard db = threadList.startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
		threadList.setOnDragDone(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT)
					&& db.hasContent(DndFormat.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) db.getContent(DND_THREADS_DATA_FORMAT);
				@SuppressWarnings("unchecked")
				final T tag = (T) db.getContent(DndFormat.TAG_DATA_FORMAT);
				addTagForThreads(tag, threads);
				event.consume();
			} else if (db.hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT)
					&& db.hasContent(DndFormat.TAG_NAME_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) db.getContent(DND_THREADS_DATA_FORMAT);
				final String name = (String) db.getContent(DndFormat.TAG_NAME_DATA_FORMAT);
				createTagForThreads(name, threads);
				event.consume();
			}
		});
		threadListPane.setCenter(threadList);

		threadListDropPane = new ThreadListDropPane<T, H, M, C>(mailService);
		threadListDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		final ThreadToolBar toolbar = new ThreadToolBar();
		toolbar.setPadding(new Insets(0, 0, 3, 0));
		toolbar.setOnReply(e -> replySelected(false));
		toolbar.setOnReplyAll(e -> replySelected(true));
		toolbar.setOnForward(e -> forwardSelected());
		toolbar.setOnToggleFlag(e -> toggleFlag());
		toolbar.setOnArchive(e -> archiveSelected());
		toolbar.setOnTrash(e -> trashSelected());
		toolbar.setOnSpam(e -> toggleSpam());

		final BooleanProperty showToolbar = Settings.getSettings().showToolbar();
		if (showToolbar.get()) {
			threadListPane.setTop(toolbar);
		}
		showToolbar.addListener((ov, o, n) -> {
			if (n) {
				threadListPane.setTop(toolbar);
			} else {
				threadListPane.getChildren().remove(toolbar);
			}
		});

		stackPane.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)
					&& !stackPane.getChildren().contains(threadListDropPane)) {
				stackPane.getChildren().add(threadListDropPane);
			}

		});
		stackPane.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)
					&& stackPane.getChildren().contains(threadListDropPane)) {
				stackPane.getChildren().remove(threadListDropPane);
			}
		});

		final ToggleGroup toggleGroup = new ToggleGroup();

		final RadioButton nameSortButton = new RadioButton("sender");
		nameSortButton.setToggleGroup(toggleGroup);

		final RadioButton dateSortButton = new RadioButton("date");
		dateSortButton.setToggleGroup(toggleGroup);

		toggleGroup.selectedToggleProperty().addListener((ChangeListener<Toggle>) (ov, oldVal, newVal) -> threadList.setOrder(nameSortButton.isSelected()
				? SortOrder.SENDER
				: SortOrder.DATE));
		dateSortButton.setSelected(true);

		final HBox sortBox = new HBox(5, new Label("sort by "), nameSortButton, dateSortButton);
		sortBox.setAlignment(Pos.CENTER);
		setBottom(sortBox);

		patternField = new ResetTextField();
		patternField.setPromptText("mail search");

		final Button newButton = new Button();
		newButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/new.png"))));
		newButton.setTooltip(new Tooltip("new mail"));
		newButton.setOnAction(event -> {
			try {
				new MailComposer<M, C>(mailService, updateHandler).newMessage("");
			} catch (final Exception e) {
				LOGGER.error("loading new message composer", e);
			}
		});

		final HBox patternBox = new HBox(patternField, newButton);
		patternBox.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(patternField, Priority.ALWAYS);
		setTop(patternBox);

		setMargin(patternField, new Insets(0, 5, 0, 0));
		setMargin(sortBox, new Insets(5));
	}

	private M getLastMessage(final H thread) throws MailException {
		String lastMessageId = null;
		for(final String id: thread.getMessageIds()) {
			lastMessageId = id;
		}
		return mailService.getMessage(lastMessageId);
	}

	private void forwardSelected() {
		try {
			for(final H t: threadList.getSelectedThreads()) {
				final M m = getLastMessage(t);
				new MailComposer<M, C>(mailService, updateHandler).forward(m);
			}
		} catch (final Exception e) {
			LOGGER.error("loading transfer composer", e);
		}
	}

	private void replySelected(final boolean all) {
		try {
			for(final H t: threadList.getSelectedThreads()) {
				final M m = getLastMessage(t);
				new MailComposer<M, C>(mailService, updateHandler).reply(m, all);
			}
		} catch (final Exception e) {
			LOGGER.error("loading reply{} composer", all? " all": "", e);
		}
	}

	private void trashSelected() {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.trash(threadList.getSelectedThreads());
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("trashing threads", e.getSource().getException()));
		task.setOnSucceeded(e -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task, "trashing threads");
	}

	private void archiveSelected() {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.archive(threadList.getSelectedThreads());
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("archiving threads", e.getSource().getException()));
		task.setOnSucceeded(e -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task, "archiving threads");
	}

	private void toggleFlag() {
		final Set<H> threads = threadList.getSelectedThreads();
		if (threads.isEmpty()) {
			return;
		}
		try {
			if (threads.iterator().next().isFlagged()) {
				removeTagForThreads(flagged, threads);
			} else {
				addTagForThreads(flagged, threads);
			}
		} catch (final Exception e) {
			LOGGER.error("adding flag", e);
		}
	}

	private void toggleSpam() {
		final Set<H> threads = threadList.getSelectedThreads();
		if (threads.isEmpty()) {
			return;
		}
		try {
			if (threads.iterator().next().getTagIds().contains(spam.getId())) {
				removeTagForThreads(spam, threads);
				addTagForThreads(inbox, threads);
			} else {
				addTagForThreads(spam, threads);
			}
		} catch (final Exception e) {
			LOGGER.error("adding flag", e);
		}
	}

	private void addTagForThreads(final T tag, final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(tag, threads);
				if (ARCHIVE_ON_DROP.get() && !tag.isSystem()) {
					mailService.archive(threads);
				}
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("adding tag {} for threads {}", tag, threads, e.getSource().getException()));
		task.setOnSucceeded(e -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task, "adding tag " + tag.getName() + " for threads");
	}

	private void removeTagForThreads(final T tag, final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.removeTagForThreads(tag, threads);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("removing tag {}", tag.getName(), e.getSource().getException()));
		task.setOnSucceeded(e -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task, "removing tag " + tag.getName());
	}

	private void createTagForThreads(final String name, final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(mailService.addTag(name), threads);
				if (ARCHIVE_ON_DROP.get()) {
					mailService.archive(threads);
				}
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("creating tag {}", name, e.getSource().getException()));
		task.setOnSucceeded(e -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task, "creating tag " + name);
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
		for(final H t: getItems()) {
			for(final String id: t.getTagIds()) {
				try {
					tags.add(mailService.getTag(id));
				} catch (final MailException e) {
					LOGGER.error("getting tag {}", id, e);
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
		threadListDropPane.setOnUpdate(handler);
		threadList.setOnUpdate(handler);
	}
}
