package net.anfoya.mail.browser.javafx.thread;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.message.MessagePane;
import net.anfoya.mail.browser.javafx.settings.SettingsDialog;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.SpecialTag;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.javafx.scene.section.SelectedTagsPane;

public class ThreadPane<T extends Tag, H extends Thread, M extends Message, C extends Contact> extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPane.class);

    private static final Image FLAG = new Image(ThreadPane.class.getResourceAsStream("/net/anfoya/mail/image/mini_flag.png"));
    private static final Image ATTACHMENT = new Image(ThreadPane.class.getResourceAsStream("/net/anfoya/mail/image/mini_attach.png"));

	private final MailService<? extends Section, T, H, M, C> mailService;

	private final HBox iconBox;
	private final TextField subjectField;
	private final SelectedTagsPane<T> tagsPane;
	private final VBox messagesBox;

	private Set<H> threads;
	private H thread;

	private final ScrollPane scrollPane;
	private final EventHandler<ScrollEvent> webScrollHandler;

	private final ObservableList<Node> messagePanes;

	private EventHandler<ActionEvent> updateHandler;
	private EventHandler<ActionEvent> logoutHandler;

	public ThreadPane(final MailService<? extends Section, T, H, M, C> mailService) {
		this.mailService = mailService;

		iconBox = new HBox(5);
		iconBox.setAlignment(Pos.CENTER_LEFT);
		iconBox.setPadding(new Insets(0,0,0, 5));

		subjectField = new TextField();
		subjectField.setFocusTraversable(false);
		subjectField.setPromptText("select a mail");
		subjectField.prefWidthProperty().bind(widthProperty());
		subjectField.setEditable(false);

		final Button settingsButton = new Button();
		settingsButton.setFocusTraversable(false);
		settingsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/settings.png"))));
		settingsButton.setTooltip(new Tooltip("settings"));
		settingsButton.setOnAction(event -> new SettingsDialog(mailService).show());

		final Button signoutButton = new Button();
		signoutButton.setFocusTraversable(false);
		signoutButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/signout.png"))));
		signoutButton.setTooltip(new Tooltip("sign out"));
		signoutButton.setOnAction(event -> logoutHandler.handle(null));

		final HBox subjectBox = new HBox(iconBox, subjectField, settingsButton, signoutButton);
		setTop(subjectBox);

		final StackPane stackPane = new StackPane();
		stackPane.setAlignment(Pos.BOTTOM_CENTER);

		final ThreadDropPane<H, M> threadDropPane = new ThreadDropPane<H, M>(mailService);
		threadDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		stackPane.setOnDragEntered(e -> {
			if ((e.getDragboard().hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT) || e.getDragboard().hasContent(ThreadDropPane.MESSAGE_DATA_FORMAT))
					&& !stackPane.getChildren().contains(threadDropPane)) {
				threadDropPane.init(e.getDragboard());
				stackPane.getChildren().add(threadDropPane);
			}
		});
		stackPane.setOnDragExited(e -> {
			if ((e.getDragboard().hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT) || e.getDragboard().hasContent(ThreadDropPane.MESSAGE_DATA_FORMAT))
					&& stackPane.getChildren().contains(threadDropPane)) {
				stackPane.getChildren().remove(threadDropPane);
			}
		});
		setCenter(stackPane);

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

		stackPane.getChildren().add(scrollPane);

		tagsPane = new SelectedTagsPane<T>();
		setBottom(tagsPane);
	}

	public void setOnUpdateThread(final EventHandler<ActionEvent> handler) {
		this.updateHandler = handler;
	}

	public void setOnLogout(final EventHandler<ActionEvent> handler) {
		logoutHandler = handler;
	}

	public void refresh(final Set<H> threads) {
		this.threads = threads;

		refreshIcons();
		refreshSubject();
		refreshThread();
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
		if (thread.equals(loadedThread)) {
			refreshCurrentThread();
		} else {
			loadThread();
		}
	}

	private void refreshTags() {
		tagsPane.clear();

		if (threads == null || threads.size() == 0) {
			return;
		}

		Platform.runLater(() -> {
			//retrieve all tags for all threads
			final Set<T> tags = new LinkedHashSet<T>();
			for(final H t: threads) {
				for(final String id: t.getTagIds()) {
					try {
						tags.add(mailService.getTag(id));
					} catch (final MailException e) {
						LOGGER.error("getting tag {}", id, e);
					}
				}
			}

			tagsPane.setRemoveTagCallBack(t -> remove(threads, t));
			tagsPane.refresh(tags);
		});
	}

	private void refreshIcons() {
		iconBox.getChildren().clear();
		if (threads.size() == 1 && threads.iterator().next().isFlagged()) {
			displayFlagIcon();
		}
	}

	private void refreshSubject() {
		switch (threads.size()) {
		case 0:
			subjectField.setText("");
			break;
		case 1:
			subjectField.setText(threads.iterator().next().getSubject());
			break;
		default:
			subjectField.setText("multiple mails selected");
			break;
		}
	}

	private void refreshCurrentThread() {
		final Set<String> messageIds = thread.getMessageIds();
		for (final Iterator<Node> i = messagePanes.iterator(); i.hasNext();) {
			@SuppressWarnings("unchecked")
			final String id = ((MessagePane<M, C>) i.next()).getMessageId();
			if (!messageIds.contains(id)) {
				i.remove();
			}
		}

		int index = 0;
		for(final Iterator<String> i=new LinkedList<String>(thread.getMessageIds()).descendingIterator(); i.hasNext();) {
			final String id = i.next();
			@SuppressWarnings("unchecked")
			final MessagePane<M, C> messagePane = index >= messagePanes.size()? null: (MessagePane<M, C>) messagePanes.get(index);
			if (messagePane != null && messagePane.hasAttachment()) {
				displayAttachmentIcon();
			}
			if (messagePane == null || !id.equals(messagePane.getMessageId())) {
				messagePanes.add(index, createMessagePane(id));
			}
			index++;
		}
	}

	private MessagePane<M, C> createMessagePane(final String id) {
		final MessagePane<M, C> messagePane = new MessagePane<M, C>(id, mailService);
		messagePane.focusTraversableProperty().bind(focusTraversableProperty());
		messagePane.setScrollHandler(webScrollHandler);
		messagePane.setUpdateHandler(updateHandler);
		messagePane.setExpanded(false);
		messagePane.onContainAttachment(e -> displayAttachmentIcon());
		messagePane.load();

		return messagePane;
	}

	private void displayAttachmentIcon() {
		for(final Node n: iconBox.getChildren()) {
			if (n instanceof ImageView && ((ImageView)n).getImage() == ATTACHMENT) {
				return;
			}
		}
		iconBox.getChildren().add(0, new ImageView(ATTACHMENT));
	}

	private void displayFlagIcon() {
		for(final Node n: iconBox.getChildren()) {
			if (n instanceof ImageView && ((ImageView)n).getImage() == FLAG) {
				return;
			}
		}
		iconBox.getChildren().add(new ImageView(FLAG));
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
			try {
				final T unread = mailService.getSpecialTag(SpecialTag.UNREAD);
				remove(threads, unread);
			} catch (final MailException e) {
				LOGGER.error("getting unread tag", e);
			}
		}
	}

	private Void remove(final Set<H> threads, final T tag) {
		ThreadPool.getInstance().submitHigh(() -> {
			try {
				mailService.removeTagForThreads(tag, threads);
			} catch (final MailException e) {
				LOGGER.error("removing tag {} for threads", tag);
			}
			updateHandler.handle(null);
		}, "removing tag " + tag.getName());
		return null;
	}
}
