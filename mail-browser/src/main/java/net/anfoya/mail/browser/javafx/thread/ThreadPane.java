package net.anfoya.mail.browser.javafx.thread;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.VoidCallback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.browser.controller.vo.TagForThreadsVo;
import net.anfoya.mail.browser.javafx.BrowserToolBar;
import net.anfoya.mail.browser.javafx.message.MessagePane;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.model.Contact;
import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Section;
import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.javafx.scene.section.SelectedTagsPane;
import net.anfoya.tag.model.SpecialTag;

public class ThreadPane<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends VBox {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPane.class);

	private static final String IMG_PATH = "/net/anfoya/mail/img/";
	private static final String FLAG_PNG = ThreadPane.class.getResource(IMG_PATH + "mini_flag.png").toExternalForm();
	private static final String ATTACH_PNG = ThreadPane.class.getResource(IMG_PATH + "mini_attach.png").toExternalForm();

	private static final Image FLAG_ICON = new Image(FLAG_PNG);
	private static final Image ATTACH_ICON = new Image(ATTACH_PNG);

	private final MailService<S, T, H, M, C> mailService;

	private final BrowserToolBar browserToolBar;
	private final ThreadToolBar threadToolBar;

	private final HBox iconBox;
	private final TextField subjectField;
	private final SelectedTagsPane<T> tagsPane;
	private final VBox messagesBox;

	private Set<H> threads;
	private H thread;

	private final ScrollPane scrollPane;
	private final EventHandler<ScrollEvent> webScrollHandler;

	private final ObservableList<Node> messagePanes;

	private final T unread;
	private final String unreadTagId;
	private final String sentTagId;

	private Task<Set<T>> tagsTask;
	private final AtomicLong tagsTaskId;

	private VoidCallback<String> openUrlCallback;

	private VoidCallback<Set<H>> markReadCallback;
	private VoidCallback<TagForThreadsVo<T, H>> removeTagForThreadsCallback;

	public ThreadPane(final MailService<S, T, H, M, C> mailService
			, final UndoService undoService
			, final Settings settings) {
		getStyleClass().add("thread");
		this.mailService = mailService;

		threads = Collections.emptySet();

		unread = mailService.getSpecialTag(SpecialTag.UNREAD);
		unreadTagId = unread.getId();
		sentTagId = mailService.getSpecialTag(SpecialTag.SENT).getId();

		tagsTaskId = new AtomicLong();

		threadToolBar = new ThreadToolBar();
		threadToolBar.setFocusTraversable(false);
		threadToolBar.setPadding(new Insets(0));

		iconBox = new HBox(5);
		iconBox.setAlignment(Pos.CENTER_LEFT);
		iconBox.setPadding(new Insets(0,0,0, 5));

		subjectField = new TextField();
		subjectField.setFocusTraversable(false);
		subjectField.setPromptText("select a thread");
		subjectField.prefWidthProperty().bind(widthProperty());
		subjectField.setEditable(false);

		browserToolBar = new BrowserToolBar();
		browserToolBar.setVisibles(false, true, true);

		final HBox subjectBox = new HBox(iconBox, subjectField, browserToolBar);
		getChildren().add(subjectBox);

		scrollPane = new ScrollPane();
		scrollPane.setFocusTraversable(false);
		scrollPane.setFitToWidth(true);
		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.getStyleClass().add("box-underline");

		messagesBox = new VBox();
		messagesBox.minHeightProperty().bind(scrollPane.heightProperty());
		scrollPane.setContent(messagesBox);
		messagePanes = messagesBox.getChildren();

		webScrollHandler = e -> {
			final double current = scrollPane.getVvalue();
			final double maxPx = messagesBox.getHeight();
			final double offset = e.getDeltaY() / maxPx * -1;
			scrollPane.setVvalue(current + offset);
			e.consume();

			LOGGER.debug("[e {}, delta {}], [max {}, delta {}]"
					, maxPx
					, e.getDeltaY()
					, scrollPane.getVmax()
					, offset);
		};

		final StackPane stackPane = new StackPane(scrollPane);
		stackPane.setAlignment(Pos.BOTTOM_CENTER);
		getChildren().add(stackPane);
		VBox.setVgrow(stackPane, Priority.ALWAYS);

		tagsPane = new SelectedTagsPane<>();
		tagsPane.setRemoveTagCallBack(t -> removeTagForThreadsCallback.call(new TagForThreadsVo<>(t, threads)));
		getChildren().add(tagsPane);
	}

	public void setOnSignout(Runnable callback) {
		browserToolBar.setOnSignout(callback);
	}

	public void refresh(final Set<H> threads) {
		this.threads = threads;
		refreshThread();
		refreshIcons();
		refreshSubject();
		refreshTags();
	}

	private void refreshThread() {
		if (threads.size() != 1) {
			thread = null;
			messagePanes.clear();
			return;
		}

		final H loadedThread = thread;
		thread = threads.iterator().next();
		if (thread == null) {
			return;
		}
		if (thread.equals(loadedThread)) {
			refreshCurrentThread();
		} else {
			loadThread();
		}
	}

	private synchronized void refreshTags() {
		final long taskId = tagsTaskId.incrementAndGet();
		tagsPane.clear();

		if (threads == null || threads.size() == 0) {
			return;
		}

		if (tagsTask != null) {
			tagsTask.cancel();
		}

		final String desc = "show threads' tags";
		tagsTask = new Task<Set<T>>() {
			@Override protected Set<T> call() throws Exception {
				return threads
						.stream()
						.flatMap(t -> t.getTagIds().stream())
						.filter(id -> !id.equals(sentTagId) && !id.equals(unreadTagId))
						.map(id -> {
							try {
								return mailService.getTag(id);
							} catch (final MailException e) {
								LOGGER.error("get tag {}", id, e);
								return null;
							}

						})
						.filter(t -> t != null)
						.collect(Collectors.toSet());
			}
		};
		tagsTask.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		tagsTask.setOnSucceeded(e -> {
			if (tagsTaskId.get() == taskId) {
				tagsPane.refresh(tagsTask.getValue());
			}
		});
		ThreadPool.getDefault().submit(PoolPriority.MIN, desc, tagsTask);
	}

	private void refreshIcons() {
		iconBox.getChildren().clear();
		if (threads.size() == 1 && thread.isFlagged()) {
			showIcon(FLAG_ICON);
		}
	}

	private void refreshSubject() {
		switch (threads.size()) {
		case 0:
			subjectField.setText("");
			break;
		case 1:
			final String count = thread.getMessageIds().size() < 2? "": "(" + thread.getMessageIds().size() + "x) ";
			subjectField.setText(count + thread.getSubject());
			break;
		default:
			subjectField.setText("multiple threads selected");
			break;
		}
	}

	private void refreshCurrentThread() {
		// removed messages
		final Set<String> messageIds = thread.getMessageIds();
		for (final Iterator<Node> i = messagePanes.iterator(); i.hasNext();) {
			@SuppressWarnings("unchecked")
			final String id = ((MessagePane<M, C>) i.next()).getMessageId();
			if (!messageIds.contains(id)) {
				i.remove();
			}
		}

		// added messages
		int index = 0;
		for(final Iterator<String> i=new LinkedList<>(thread.getMessageIds()).descendingIterator(); i.hasNext();) {
			final String id = i.next();
			@SuppressWarnings("unchecked")
			final MessagePane<M, C> messagePane = index >= messagePanes.size()? null: (MessagePane<M, C>) messagePanes.get(index);
			if (messagePane == null || !id.equals(messagePane.getMessageId())) {
				messagePanes.add(index, createMessagePane(id));
			}
			index++;
		}
	}

	private MessagePane<M, C> createMessagePane(final String id) {
		final MessagePane<M, C> messagePane = new MessagePane<>(id, mailService);
		messagePane.focusTraversableProperty().bind(focusTraversableProperty());
		messagePane.setScrollHandler(webScrollHandler);
		messagePane.setOnOpenUrl(openUrlCallback);
		messagePane.setExpanded(false);
		messagePane.onContainAttachment(() -> showIcon(ATTACH_ICON));
		messagePane.load();

		return messagePane;
	}

	private void showIcon(final Image icon) {
		for(final Node n: iconBox.getChildren()) {
			if (n instanceof ImageView && ((ImageView)n).getImage() == icon) {
				return;
			}
		}
		iconBox.getChildren().add(new ImageView(icon));
	}

	@SuppressWarnings("unchecked")
	private void loadThread() {
		scrollPane.setVvalue(0);
		messagePanes.clear();

		if (!thread.getMessageIds().isEmpty()) {
			for(final String id: thread.getMessageIds()) {
				messagePanes.add(0, createMessagePane(id));
			}
			MessagePane<M, C> messagePane = (MessagePane<M, C>) messagePanes.get(0);
			messagePane.setExpanded(true);
			if (messagePanes.size() == 1) {
				// only one message, not collapsible
				messagePane.setCollapsible(false);
			} else {
				// last message is not collapsible
				messagePane = (MessagePane<M, C>) messagePanes.get(messagePanes.size()-1);
				messagePane.setExpanded(true);
				messagePane.setCollapsible(false);
			}
		}

		if (thread.isUnread()) {
			markReadCallback.call(threads);
		}
	}

	public void setDetached(boolean detached) {
		browserToolBar.setVisibles(false, !detached, !detached);
		if (detached && !(getChildren().get(0) instanceof ThreadToolBar)) {
			getChildren().add(0, threadToolBar);
		} else if (!detached && getChildren().get(0) instanceof ThreadToolBar) {
			getChildren().remove(0);
		}
	}

	public void setOnReply(VoidCallback<Set<H>> callback) {
		threadToolBar.setOnReply(() -> callback.call(threads));
	}

	public void setOnReplyAll(VoidCallback<Set<H>> callback) {
		threadToolBar.setOnReplyAll(() -> callback.call(threads));
	}

	public void setOnForward(VoidCallback<Set<H>> callback) {
		threadToolBar.setOnForward(() -> callback.call(threads));
	}

	public void setOnToggleFlag(VoidCallback<Set<H>> callback) {
		threadToolBar.setOnToggleFlag(() -> callback.call(threads));
	}

	public void setOnArchive(VoidCallback<Set<H>> callback) {
		threadToolBar.setOnArchive(() -> callback.call(threads));
	}

	public void setOnTrash(VoidCallback<Set<H>> callback) {
		threadToolBar.setOnTrash(() -> callback.call(threads));
	}

	public void setOnToggleSpam(VoidCallback<Set<H>> callback) {
		threadToolBar.setOnSpam(() -> callback.call(threads));
	}
	public void setOnOpenUrl(VoidCallback<String> callback) {
		openUrlCallback = callback;
	}

	public H getThread() {
		return thread;
	}

	public boolean hasThreads(Set<H> threads) {
		return this.threads.equals(threads);
	}

	public void setOnMarkRead(VoidCallback<Set<H>> callback) {
		this.markReadCallback = callback;
	}

	public void setOnRemoveTagForThreads(VoidCallback<TagForThreadsVo<T, H>> callback) {
		this.removeTagForThreadsCallback = callback;
	}

	public void setOnShowSettings(Runnable callback) {
		browserToolBar.setOnShowSettings(callback);
	}
}
