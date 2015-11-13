package net.anfoya.mail.browser.javafx.threadlist;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
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
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.animation.DelayTimeline;
import net.anfoya.javafx.scene.control.ResetTextField;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.settings.SettingsDialog;
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
	private static final ReadOnlyBooleanProperty ARCHIVE_ON_DROP = Settings.getSettings().archiveOnDrop();

	public static final DataFormat DND_THREADS_DATA_FORMAT = new DataFormat("DND_THREADS_DATA_FORMAT");

	private final MailService<S, T, H, M, C> mailService;
	private final ThreadList<S, T, H, M, C> threadList;
	private final ResetTextField patternField;

	private final T inbox;
	private final T spam;
	private final T flagged;

	private DelayTimeline patternDelay;

	private final ThreadListDropPane threadListDropPane;

	private EventHandler<ActionEvent> updateHandler;
	private S currentSection;

	public ThreadListPane(final MailService<S, T, H, M, C> mailService) throws MailException {
		this.mailService = mailService;

		inbox = mailService.getSpecialTag(SpecialTag.INBOX);
		spam = mailService.getSpecialTag(SpecialTag.SPAM);
		flagged = mailService.getSpecialTag(SpecialTag.FLAGGED);

		patternField = new ResetTextField();
		patternField.setPromptText("mail search");

		threadList = new ThreadList<S, T, H, M, C>(mailService);
		threadList.setOnDragDetected(event -> {
	        final ClipboardContent content = new ClipboardContent();
	        content.put(ExtItemDropPane.ADD_TAG_DATA_FORMAT, "");
	        content.put(DND_THREADS_DATA_FORMAT, "");
	        final Dragboard db = threadList.startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
		threadList.setOnDragDone(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasContent(Tag.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final T tag = (T) db.getContent(Tag.TAG_DATA_FORMAT);
				addTagForThreads(tag, getSelectedThreads());
			} else if (db.hasContent(ExtItemDropPane.TAG_NAME_DATA_FORMAT)) {
				final String name = (String) db.getContent(ExtItemDropPane.TAG_NAME_DATA_FORMAT);
				createTagForThreads(name, getSelectedThreads());
			}
			e.consume();
		});


		final StackPane centerPane = new StackPane(threadList);
		centerPane.setAlignment(Pos.BOTTOM_CENTER);
		setCenter(centerPane);

		final Button settingsButton = new Button();
		settingsButton.setFocusTraversable(false);
		settingsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/settings.png"))));
		settingsButton.setTooltip(new Tooltip("settings"));
		settingsButton.setOnAction(event -> new SettingsDialog<S, T>(mailService).show());

		RotateTransition rotateTransition = new RotateTransition(Duration.millis(250), settingsButton);
		rotateTransition.setFromAngle(-15);
		rotateTransition.setToAngle(15);
		rotateTransition.setAutoReverse(true);
		rotateTransition.setCycleCount(Timeline.INDEFINITE);
		rotateTransition.setInterpolator(Interpolator.EASE_IN);
		rotateTransition.play();
		
		threadList.loadingProperty().addListener((ov, o, n) -> {
			if (n) {
				rotateTransition.play();
			} else {
				rotateTransition.stop();
				settingsButton.setRotate(0);
			}
		});
		
		final Button newButton = new Button();
		newButton.setFocusTraversable(false);
		newButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/new.png"))));
		newButton.setTooltip(new Tooltip("new mail"));
		newButton.setOnAction(event -> {
			try {
				new MailComposer<M, C>(mailService, updateHandler).newMessage("");
			} catch (final Exception e) {
				LOGGER.error("loading new message composer", e);
			}
		});

		final VBox topBox = new VBox();
		setTop(topBox);

		final HBox firstLineBox = new HBox(patternField, settingsButton, newButton);
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

		final BooleanProperty showToolbar = Settings.getSettings().showToolbar();
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

		toggleGroup.selectedToggleProperty().addListener((ChangeListener<Toggle>) (ov, oldVal, newVal) -> threadList.setOrder(nameSortButton.isSelected()
				? SortOrder.SENDER
				: SortOrder.DATE));
		dateSortButton.setSelected(true);

		final HBox sortBox = new HBox(5, new Label("sort by "), nameSortButton, dateSortButton);
		sortBox.setAlignment(Pos.CENTER);
		setBottom(sortBox);
		setMargin(sortBox, new Insets(5));
	}

	private void forwardSelected() {
		try {
			for(final H t: threadList.getSelectedThreads()) {
				final M message = mailService.getMessage(t.getLastMessageId());
				new MailComposer<M, C>(mailService, updateHandler).forward(message);
			}
		} catch (final Exception e) {
			LOGGER.error("loading transfer composer", e);
		}
	}

	private Void replySelected(final boolean all) {
		try {
			for(final H t: threadList.getSelectedThreads()) {
				final M message = mailService.getMessage(t.getLastMessageId());
				new MailComposer<M, C>(mailService, updateHandler).reply(message, all);
			}
		} catch (final Exception e) {
			LOGGER.error("loading reply{} composer", all? " all": "", e);
		}
		return null;
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
		final Set<H> threads = threadList.getSelectedThreads();
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.archive(threads);
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
				final T tag = mailService.addTag(name);
				if (currentSection != null && !currentSection.isSystem()) {
					mailService.moveToSection(tag, currentSection);
				}
				mailService.addTagForThreads(tag, threads);
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
		threadList.setOnUpdate(handler);
	}

	public void setCurrentSection(S section) {
		currentSection = section;
	}
}
