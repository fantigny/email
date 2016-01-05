package net.anfoya.mail.browser.javafx.thread;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import javafx.util.Duration;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.browser.javafx.message.MessagePane;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.settings.SettingsDialog;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.javafx.scene.section.SelectedTagsPane;
import net.anfoya.tag.model.SpecialTag;

public class ThreadPane<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPane.class);

    private static final String IMG_PATH = "/net/anfoya/mail/img/";
    private static final String SETTINGS_PNG = ThreadPane.class.getResource(IMG_PATH + "settings.png").toExternalForm();
    private static final String SIGNOUT_PNG = ThreadPane.class.getResource(IMG_PATH + "signout.png").toExternalForm();

    private static final String FLAG_PNG = ThreadPane.class.getResource(IMG_PATH + "mini_flag.png").toExternalForm();
    private static final String ATTACH_PNG = ThreadPane.class.getResource(IMG_PATH + "mini_attach.png").toExternalForm();

    private static final Image FLAG_ICON = new Image(FLAG_PNG);
    private static final Image ATTACH_ICON = new Image(ATTACH_PNG);

	private final MailService<S, T, H, M, C> mailService;
	private final UndoService undoService;
	private final Settings settings;

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

	private EventHandler<ActionEvent> updateHandler;
	private EventHandler<ActionEvent> signoutHandler;

	private SettingsDialog<S, T> settingsDialog;

	private boolean markRead;

	public ThreadPane(final MailService<S, T, H, M, C> mailService
			, final UndoService undoService
			, final Settings settings) {
		getStyleClass().add("thread");
		this.mailService = mailService;
		this.undoService = undoService;
		this.settings = settings;

		unread = mailService.getSpecialTag(SpecialTag.UNREAD);

		iconBox = new HBox(5);
		iconBox.setAlignment(Pos.CENTER_LEFT);
		iconBox.setPadding(new Insets(0,0,0, 5));

		subjectField = new TextField();
		subjectField.setFocusTraversable(false);
		subjectField.setPromptText("select a thread");
		subjectField.prefWidthProperty().bind(widthProperty());
		subjectField.setEditable(false);

		final Button settingsButton = new Button();
		settingsButton.getStyleClass().add("flat-button");
		settingsButton.setFocusTraversable(false);
		settingsButton.setGraphic(new ImageView(new Image(SETTINGS_PNG)));
		settingsButton.setTooltip(new Tooltip("settings"));
		settingsButton.setOnAction(e -> showSettings(settings));

		final Node graphics = settingsButton.getGraphic();

		final RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), graphics);
		rotateTransition.setByAngle(360);
		rotateTransition.setCycleCount(Timeline.INDEFINITE);
		rotateTransition.setInterpolator(Interpolator.EASE_IN);

		final RotateTransition stopRotateTransition = new RotateTransition(Duration.INDEFINITE, graphics);
		rotateTransition.setInterpolator(Interpolator.EASE_OUT);

		ThreadPool.getDefault().setOnChange(PoolPriority.MAX, map -> {
			if (map.isEmpty()) {
				rotateTransition.stop();
				stopRotateTransition.setByAngle(360d - graphics.getRotate() % 360d);
				stopRotateTransition.setDuration(Duration.seconds(.5 * stopRotateTransition.getByAngle() / 360d));
				stopRotateTransition.play();
			} else {
				stopRotateTransition.stop();
				rotateTransition.play();
			}
		});

		final Button signoutButton = new Button();
		signoutButton.getStyleClass().add("flat-button");
		signoutButton.setFocusTraversable(false);
		signoutButton.setGraphic(new ImageView(new Image(SIGNOUT_PNG)));
		signoutButton.setTooltip(new Tooltip("sign out"));
		signoutButton.setOnAction(e -> signoutHandler.handle(null));

		final HBox subjectBox = new HBox(iconBox, subjectField, settingsButton, signoutButton);
		setTop(subjectBox);

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
		setCenter(stackPane);

		tagsPane = new SelectedTagsPane<T>();
		setBottom(tagsPane);
	}

	private void showSettings(final Settings settings) {
		if (settingsDialog == null
				|| !settingsDialog.isShowing()) {
			settingsDialog = new SettingsDialog<S, T>(mailService, undoService, settings);
			settingsDialog.show();
		}
		settingsDialog.toFront();
		settingsDialog.requestFocus();
	}

	public void setOnUpdateThread(final EventHandler<ActionEvent> handler) {
		this.updateHandler = handler;
	}

	public void setOnSignout(final EventHandler<ActionEvent> handler) {
		signoutHandler = handler;
	}

	public void refresh(final Set<H> threads, final boolean markRead) {
		this.threads = threads;
		this.markRead = markRead;

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
						LOGGER.error("get tag {}", id, e);
					}
				}
			}

			tagsPane.setRemoveTagCallBack(t -> remove(threads, t, true));
			tagsPane.refresh(tags);
		});
	}

	private void refreshIcons() {
		iconBox.getChildren().clear();
		if (threads.size() == 1 && threads.iterator().next().isFlagged()) {
			showIcon(FLAG_ICON);
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
				showIcon(ATTACH_ICON);
			}
			if (messagePane == null || !id.equals(messagePane.getMessageId())) {
				messagePanes.add(index, createMessagePane(id));
			}
			index++;
		}
	}

	private MessagePane<M, C> createMessagePane(final String id) {
		final MessagePane<M, C> messagePane = new MessagePane<M, C>(id, mailService, settings);
		messagePane.focusTraversableProperty().bind(focusTraversableProperty());
		messagePane.setScrollHandler(webScrollHandler);
		messagePane.setUpdateHandler(updateHandler);
		messagePane.setExpanded(false);
		messagePane.onContainAttachment(e -> showIcon(ATTACH_ICON));
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

		if (markRead && thread.isUnread()) {
			remove(threads, unread, false);
		}
	}

	private Void remove(final Set<H> threads, final T tag, final boolean undo) {
		final String desc = String.format("remove tag %s", tag);
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.removeTagForThreads(tag, threads);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error(desc, tag, threads, e.getSource().getException()));
		task.setOnSucceeded(e -> {
			if (undo) {
				undoService.set(
						() -> { mailService.addTagForThreads(tag, threads); return null; }
						, desc);
			}
			updateHandler.handle(null);
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
		return null;
	}
}
