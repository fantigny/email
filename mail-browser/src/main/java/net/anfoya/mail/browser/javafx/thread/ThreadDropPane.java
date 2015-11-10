package net.anfoya.mail.browser.javafx.thread;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Thread;

public class ThreadDropPane<H extends Thread, M extends Message> extends GridPane {
	public static final DataFormat MESSAGE_DATA_FORMAT = new DataFormat("MESSAGE_DATA_FORMAT");
	private final DropArea newThreadArea;
	private final DropArea addThreadArea;

	public ThreadDropPane() {
		setMaxHeight(50);
		getStyleClass().add("droparea-grid");

		addThreadArea = new DropArea("add to thread", DND_THREADS_DATA_FORMAT);
		addThreadArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				addThreads(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		newThreadArea = new DropArea("create new thread", MESSAGE_DATA_FORMAT);
		newThreadArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(MESSAGE_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final M message = (M) event.getDragboard().getContent(MESSAGE_DATA_FORMAT);
				newThread(message);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		setHgrow(newThreadArea, Priority.ALWAYS);
		setVgrow(newThreadArea, Priority.ALWAYS);
		setHgrow(addThreadArea, Priority.ALWAYS);
		setVgrow(addThreadArea, Priority.ALWAYS);

	}

	public void init(final Dragboard db) {
		getChildren().clear();

		if (db.hasContent(MESSAGE_DATA_FORMAT)) {
			addRow(0, newThreadArea);
		}
		if (db.hasContent(DND_THREADS_DATA_FORMAT)) {
			addRow(0, addThreadArea);
		}
	}

	private void newThread(final M message) {
		// TODO Auto-generated method stub

	}


	private void addThreads(final Set<H> threads) {
		// TODO Auto-generated method stub

	}
}
