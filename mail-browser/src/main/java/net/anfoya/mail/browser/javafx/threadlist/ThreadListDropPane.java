package net.anfoya.mail.browser.javafx.threadlist;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.mail.service.MailException;

public class ThreadListDropPane extends GridPane {
	private EventHandler<ActionEvent> archiveHandler;
	private EventHandler<ActionEvent> forwardHandler;

	private Callback<Boolean, Void> replyCallback;

	public ThreadListDropPane() throws MailException {
		setMaxHeight(200);
		getStyleClass().add("droparea-grid");

		final DropArea archiveArea = new DropArea("archive", DND_THREADS_DATA_FORMAT);
		archiveArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				archiveHandler.handle(null);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea replyArea = new DropArea("reply", DND_THREADS_DATA_FORMAT);
		replyArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				replyCallback.call(false);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea replyAllArea = new DropArea("reply all", DND_THREADS_DATA_FORMAT);
		replyArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				replyCallback.call(true);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea forwardArea = new DropArea("forward", DND_THREADS_DATA_FORMAT);
		forwardArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				forwardHandler.handle(null);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		int i=0;
		addRow(i++, replyArea);
		addRow(i++, replyAllArea, forwardArea);
		addRow(i++, archiveArea);

		setColumnSpan(replyArea, 2);
		setHgrow(replyArea, Priority.ALWAYS);
		setVgrow(replyArea, Priority.ALWAYS);

		setHgrow(replyAllArea, Priority.ALWAYS);
		setVgrow(replyAllArea, Priority.ALWAYS);
		setHgrow(forwardArea, Priority.ALWAYS);
		setVgrow(forwardArea, Priority.ALWAYS);

		setColumnSpan(archiveArea, 2);
		setHgrow(archiveArea, Priority.ALWAYS);
		setVgrow(archiveArea, Priority.ALWAYS);
	}

	public void setOnReply(Callback<Boolean, Void> callback) {
		replyCallback = callback;
	}

	public void setOnForward(EventHandler<ActionEvent> handler) {
		forwardHandler = handler;
	}

	public void setOnArchive(final EventHandler<ActionEvent> handler) {
		this.archiveHandler = handler;
	}
}
