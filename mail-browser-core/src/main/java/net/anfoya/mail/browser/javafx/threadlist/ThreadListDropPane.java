package net.anfoya.mail.browser.javafx.threadlist;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.mail.browser.javafx.message.MessageComposer;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadListDropPane<T extends SimpleTag, H extends SimpleThread, M extends SimpleMessage> extends GridPane {
	private final MailService<? extends SimpleSection, T, H, M> mailService;
	private EventHandler<ActionEvent> updateHandler;

	public ThreadListDropPane(final MailService<? extends SimpleSection, T, H, M> mailService) {
		this.mailService = mailService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(180);

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

		final DropArea replyArea = new DropArea("reply", DND_THREADS_DATA_FORMAT);
		replyArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				reply(threads, false);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea replyAllArea = new DropArea("reply all", DND_THREADS_DATA_FORMAT);
		replyArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				reply(threads, true);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea forwardArea = new DropArea("forward", DND_THREADS_DATA_FORMAT);
		forwardArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				forward(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea unreadArea = new DropArea("unread", DND_THREADS_DATA_FORMAT);
		unreadArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				unread(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		addRow(0, archiveArea, deleteArea);
		addRow(1, replyArea, replyAllArea);
		addRow(2, forwardArea, unreadArea);
	}

	private void forward(final Set<H> threads) {
		try {
			final M message = mailService.getMessage(threads.iterator().next().getLastMessageId());
			new MessageComposer<M>(mailService, updateHandler).forward(message);
		} catch (final MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void reply(final Set<H> threads, final boolean all) {
		try {
			final M message = mailService.getMessage(threads.iterator().next().getLastMessageId());
			new MessageComposer<M>(mailService, updateHandler).reply(message, all);
		} catch (final MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void unread(final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(mailService.findTag("UNREAD"), threads);
				return null;
			}
		};
		task.setOnFailed(event -> { /* TODO*/ });
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void archive(final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.archive(threads);
				return null;
			}
		};
		task.setOnFailed(event -> { /* TODO*/ });
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void delete(final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.trash(threads);
				return null;
			}
		};
		task.setOnFailed(event -> { /* TODO*/ });
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	public void setOnUpdate(final EventHandler<ActionEvent> handler) {
		this.updateHandler = handler;
	}
}
