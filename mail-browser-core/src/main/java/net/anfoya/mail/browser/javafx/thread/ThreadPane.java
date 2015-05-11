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
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
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

public class ThreadPane<T extends SimpleTag, H extends SimpleThread, M extends SimpleMessage> extends BorderPane {
	private final MailService<? extends SimpleSection, T, H, M> mailService;

	private final TextField subjectField;
	private final VBox messagesBox;
	private final SelectedTagsPane<T> tagsPane;

	private EventHandler<ActionEvent> delTagHandler;

	private Set<H> threads;
	private H thread;

	private final ScrollPane scrollPane;

	public ThreadPane(final MailService<? extends SimpleSection, T, H, M> mailService) {
		this.mailService = mailService;

		setPadding(new Insets(5));

		subjectField = new TextField("select a thread");
		subjectField.setEditable(false);
		setTop(subjectField);

		final StackPane stackPane = new StackPane();
		stackPane.setAlignment(Pos.BOTTOM_CENTER);
		BorderPane.setMargin(stackPane, new Insets(5, 0, 5, 0));

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
		scrollPane.getStyleClass().add("edge-to-edge");

		messagesBox = new VBox();
		messagesBox.minHeightProperty().bind(scrollPane.heightProperty());
		scrollPane.setContent(messagesBox);

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
			messagesBox.getChildren().clear();
			return;
		}

		final H previous = thread;
		thread = threads.iterator().next();
		if (previous != null && previous.getId().equals(thread.getId())) {
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
		for (final Iterator<Node> i = messagesBox.getChildren().iterator(); i.hasNext();) {
			@SuppressWarnings("unchecked")
			final MessagePane<M> messagePane = (MessagePane<M>) i.next();
			if (!thread.getMessageIds().contains(messagePane.getMessage().getId())) {
				i.remove();
			}
		}

		int index = 0;
		boolean expanded = false;
		final ObservableList<Node> panes = messagesBox.getChildren();
		for(final Iterator<String> i=new LinkedList<String>(thread.getMessageIds()).descendingIterator(); i.hasNext();) {
			final String id = i.next();
			@SuppressWarnings("unchecked")
			final
			MessagePane<M> messagePane = index < panes.size()? (MessagePane<M>) messagesBox.getChildren().get(index): null;
			if (messagePane == null || !id.equals(messagePane.getMessage().getId())) {
				messagePane.setParentScrollPane(scrollPane);
				panes.add(index, messagePane);
				messagePane.refresh();
				index++;
			}
			index++;
			expanded = expanded || messagePane.isExpanded();
		}

		if (!messagesBox.getChildren().isEmpty() && !expanded) {
			((TitledPane)messagesBox.getChildren().get(0)).setExpanded(true);
		}
	}

	private void loadThread() {
		messagesBox.getChildren().clear();
		for(final String id: thread.getMessageIds()) {
			final MessagePane<M> messagePane = new MessagePane<M>(id, mailService);
			messagePane.setParentScrollPane(scrollPane);
			messagePane.refresh();
			messagesBox.getChildren().add(0, messagePane);
		}

		if (!messagesBox.getChildren().isEmpty()) {
			((TitledPane)messagesBox.getChildren().get(0)).setExpanded(true);
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

	public void setOnDelTag(final EventHandler<ActionEvent> handler) {
		this.delTagHandler = handler;
	}

	public void refreshTags() {
		if (threads.size() == 0) {
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
