package net.anfoya.mail.browser.javafx.thread;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.javafx.scene.section.SelectedTagsPane;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadPane<T extends SimpleTag, H extends SimpleThread, M extends SimpleMessage> extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPane.class);

	private final MailService<? extends SimpleSection, T, H, M> mailService;

	private final TextField subjectField;
	private final SelectedTagsPane<T> tagsPane;
	private final VBox messagesBox;

	private EventHandler<ActionEvent> delTagHandler;

	private Set<H> threads;
	private H thread;

	private final ScrollPane scrollPane;
	private final EventHandler<ScrollEvent> webScrollHandler = event -> {
		final double current = scrollPane.getVvalue();
		final double maxPx = messagesBox.getHeight();
		final double offset = event.getDeltaY() / maxPx * -1;
		scrollPane.setVvalue(current + offset);
		event.consume();

		LOGGER.debug("[max {}, delta {}], [max {}, delta {}]"
				, maxPx
				, event.getDeltaY()
				, scrollPane.getVmax()
				, offset);
	};

	private final ObservableList<Node> msgPanes;

	public ThreadPane(final MailService<? extends SimpleSection, T, H, M> mailService) {
		this.mailService = mailService;

		subjectField = new TextField("select a thread");
		subjectField.prefWidthProperty().bind(widthProperty());
		subjectField.setEditable(false);
		final HBox subjectBox = new HBox(subjectField);
		subjectBox.setPadding(new Insets(0, 5, 0, 5));
		setTop(subjectBox);

		final StackPane stackPane = new StackPane();
		stackPane.setAlignment(Pos.BOTTOM_CENTER);
		stackPane.setPadding(new Insets(5, 0, 5, 0));

		final ThreadDropPane<H, M> threadDropPane = new ThreadDropPane<H, M>(mailService);
		threadDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		stackPane.setOnDragEntered(event -> {
			if ((event.getDragboard().hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT) || event.getDragboard().hasContent(ThreadDropPane.MESSAGE_DATA_FORMAT))
					&& !stackPane.getChildren().contains(threadDropPane)) {
				threadDropPane.init(event.getDragboard());
				stackPane.getChildren().add(threadDropPane);
			}
		});
		stackPane.setOnDragExited(event -> {
			if ((event.getDragboard().hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT) || event.getDragboard().hasContent(ThreadDropPane.MESSAGE_DATA_FORMAT))
					&& stackPane.getChildren().contains(threadDropPane)) {
				stackPane.getChildren().remove(threadDropPane);
			}
		});
		stackPane.setOnDragDone(event -> {
			//TODO
		});
		setCenter(stackPane);

		scrollPane = new ScrollPane();
		scrollPane.setFitToWidth(true);
//		scrollPane.getStyleClass().add("edge-to-edge");

		messagesBox = new VBox();
		messagesBox.minHeightProperty().bind(scrollPane.heightProperty());
		scrollPane.setContent(messagesBox);
		msgPanes = messagesBox.getChildren();

		stackPane.getChildren().add(scrollPane);

		tagsPane = new SelectedTagsPane<T>();
		setBottom(tagsPane);
	}

	public void refresh(final Set<H> threads) {
		this.threads = threads;

		refreshSubject();
		refreshThread();
		refreshTags();
	}

	private void refreshThread() {
		if (threads.size() != 1) {
			thread = null;
			msgPanes.clear();
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

	private void refreshSubject() {
		switch (threads.size()) {
		case 0:
			subjectField.setText("select a thread");
			break;
		case 1:
			subjectField.setText(threads.iterator().next().getSubject());
			break;
		default:
			subjectField.setText("multiple thread selected");
			break;
		}
	}

	private void refreshCurrentThread() {
		final Set<String> messageIds = thread.getMessageIds();
		for (final Iterator<Node> i = msgPanes.iterator(); i.hasNext();) {
			@SuppressWarnings("unchecked")
			final String id = ((MessagePane<M>) i.next()).getMessageId();
			if (!messageIds.contains(id)) {
				i.remove();
			}
		}

		int index = 0;
		for(final Iterator<String> i=new LinkedList<String>(thread.getMessageIds()).descendingIterator(); i.hasNext();) {
			final String id = i.next();
			@SuppressWarnings("unchecked")
			MessagePane<M> messagePane = index < msgPanes.size()? (MessagePane<M>) msgPanes.get(index): null;
			if (messagePane == null || !id.equals(messagePane.getMessageId())) {
				messagePane = new MessagePane<M>(id, mailService);
				messagePane.setScrollHandler(webScrollHandler);
				msgPanes.add(index, messagePane);
				messagePane.load();
			}
			index++;
		}
	}

	private void loadThread() {
		scrollPane.setVvalue(0);
		msgPanes.clear();
		for(final String id: thread.getMessageIds()) {
			final MessagePane<M> messagePane = new MessagePane<M>(id, mailService);
			messagePane.setScrollHandler(webScrollHandler);
			messagePane.load();

			msgPanes.add(0, messagePane);
			if (msgPanes.size() == 1) {
				// last item is not collapsible
				messagePane.setCollapsible(false);
			}
		}

		try {
			final T unread = mailService.findTag("UNREAD");
			if (thread.getTagIds().contains(unread.getId())) {
				mailService.removeTagForThread(unread, thread);
				delTagHandler.handle(null);
			}
		} catch (final MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setOnUpdateThread(final EventHandler<ActionEvent> handler) {
		this.delTagHandler = handler;
	}

	public void refreshTags() {
		if (threads == null || threads.size() == 0) {
			tagsPane.clear();
			return;
		}

		final Set<T> tags = new LinkedHashSet<T>();
		for(final H t: threads) {
			for(final String id: t.getTagIds()) {
				try {
					tags.add(mailService.getTag(id));
				} catch (final MailException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			tagsPane.setDelTagCallBack(tag -> {
				try {
					mailService.removeTagForThread(tag, t);
				} catch (final MailException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				delTagHandler.handle(null);
				return null;
			});
			tagsPane.refresh(tags);
		}
	}
}
