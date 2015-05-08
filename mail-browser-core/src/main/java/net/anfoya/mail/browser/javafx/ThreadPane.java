package net.anfoya.mail.browser.javafx;

import java.util.LinkedHashSet;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import net.anfoya.mail.browser.javafx.dnd.ThreadDropPane;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.javafx.scene.control.SelectedTagsPane;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagServiceException;

public class ThreadPane<T extends SimpleTag, H extends SimpleThread, M extends SimpleMessage> extends BorderPane {
	private final MailService<? extends SimpleSection, T, H, M> mailService;

	private final TextField subjectField;
	private final Accordion messageAcc;
	private final SelectedTagsPane<T> tagsPane;

	private EventHandler<ActionEvent> delTagHandler;
	private Set<T> tags;

	public ThreadPane(final MailService<? extends SimpleSection, T, H, M> mailService) {
		this.mailService = mailService;

		setPadding(new Insets(5));

		subjectField = new TextField("select a thread");
		subjectField.setEditable(false);
		setTop(subjectField);

		messageAcc = new Accordion();
		BorderPane.setMargin(messageAcc, new Insets(5, 0, 5, 0));

		final StackPane stackPane = new StackPane(messageAcc);
		stackPane.setAlignment(Pos.BOTTOM_CENTER);

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


		tagsPane = new SelectedTagsPane<T>();
		setBottom(tagsPane);
	}

	public void refresh(final Set<H> threads) {
		messageAcc.getPanes().clear();
		tagsPane.clear();
		switch (threads.size()) {
		case 0:
			subjectField.setText("select a thread");
			break;
		case 1:
			refresh(threads.iterator().next());
			break;
		default:
			subjectField.setText("multiple thread selected");
			break;
		}
	}

	private void refresh(final H thread) {
		subjectField.setText(thread.getSubject());

		for(final String id: thread.getMessageIds()) {
			final MessagePane<M> pane = new MessagePane<M>(mailService);
			messageAcc.getPanes().add(0, pane);
			pane.load(id);
		}

		if (!messageAcc.getPanes().isEmpty()) {
			messageAcc.setExpandedPane(messageAcc.getPanes().get(0));
		}

		refreshTags(thread);
	}

	public void setOnDelTag(final EventHandler<ActionEvent> handler) {
		this.delTagHandler = handler;
	}

	public void refreshTags(final H thread) {
		final Set<T> tags = new LinkedHashSet<T>();
		for(final String id: thread.getTagIds()) {
			try {
				tags.add(mailService.getTag(id));
			} catch (final TagServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		tagsPane.setDelTagCallBack(new Callback<T, Void>() {
			@Override
			public Void call(final T tag) {
				try {
					mailService.remTag(tag, thread);
				} catch (final MailServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				delTagHandler.handle(null);
				return null;
			}

		});
		refreshTags(tags);
	}

	public void refreshTags(final Set<T> tags) {
		this.tags = tags;
		refreshTags();
	}

	public void refreshTags() {
		tagsPane.refresh(tags);
	}
}
