package net.anfoya.mail.browser.javafx.thread;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.geometry.Pos;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.GridPane;

import javax.mail.internet.MimeMessage;

import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

public class ThreadDropPane<H extends Thread, M extends Message> extends GridPane {
	public static final DataFormat MESSAGE_DATA_FORMAT = new DataFormat(MimeMessage.class.getCanonicalName());
	@SuppressWarnings("unused")
	private final MailService<? extends Section, ? extends Tag, H, M, ? extends Contact> mailService;
	private final DropArea newThreadArea;
	private final DropArea addThreadArea;

	public ThreadDropPane(final MailService<? extends Section, ? extends Tag, H, M, ? extends Contact> tagService) {
		this.mailService = tagService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(60);

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
	}

	private void newThread(final M message) {
		// TODO Auto-generated method stub

	}


	private void addThreads(final Set<H> threads) {
		// TODO Auto-generated method stub

	}

	public void init(final Dragboard db) {
		if (getChildren().contains(newThreadArea)) {
			getChildren().remove(newThreadArea);
		}
		if (getChildren().contains(addThreadArea)) {
			getChildren().remove(addThreadArea);
		}

		if (db.hasContent(MESSAGE_DATA_FORMAT)) {
			addRow(0, newThreadArea);
		}
		if (db.hasContent(DND_THREADS_DATA_FORMAT)) {
			addRow(0, addThreadArea);
		}
	}
}
