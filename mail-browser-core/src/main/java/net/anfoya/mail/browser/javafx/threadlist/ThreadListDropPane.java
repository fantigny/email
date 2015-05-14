package net.anfoya.mail.browser.javafx.threadlist;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadListDropPane<H extends SimpleThread> extends GridPane {
	private final MailService<? extends SimpleSection, ? extends SimpleTag, H, ? extends SimpleMessage> mailService;
	private EventHandler<ActionEvent> onUpdateHandler;

	public ThreadListDropPane(final MailService<? extends SimpleSection, ? extends SimpleTag, H, ? extends SimpleMessage> tagService) {
		this.mailService = tagService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(60);

		final DropArea archiveArea = new DropArea("archive", DND_THREADS_DATA_FORMAT);
		archiveArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				archive(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea deleteArea = new DropArea("delete", DND_THREADS_DATA_FORMAT);
		deleteArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				delete(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		addRow(0, archiveArea, deleteArea);
	}

	private void archive(final Set<H> threads) {
		try {
			mailService.archive(threads);
			onUpdateHandler.handle(null);
		} catch (final MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void delete(final Set<H> threads) {
		try {
			mailService.trash(threads);
			onUpdateHandler.handle(null);
		} catch (final MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setOnUpdate(final EventHandler<ActionEvent> handler) {
		this.onUpdateHandler = handler;
	}
}
