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
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadListDropPane<T extends SimpleTag, H extends SimpleThread, M extends SimpleMessage, C extends SimpleContact> extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadListDropPane.class);

	private final MailService<? extends SimpleSection, T, H, M, C> mailService;
	private EventHandler<ActionEvent> updateHandler;

	public ThreadListDropPane(final MailService<? extends SimpleSection, T, H, M, C> mailService) {
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

		final DropArea starArea = new DropArea("star", DND_THREADS_DATA_FORMAT);
		starArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				star(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final DropArea trashArea = new DropArea("trash", DND_THREADS_DATA_FORMAT);
		trashArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				trash(threads);
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

		int i=0;
		addRow(i++, starArea);
		addRow(i++, archiveArea, trashArea);
		addRow(i++, replyArea, replyAllArea);
		addRow(i++, forwardArea, unreadArea);
	}

	private void forward(final Set<H> threads) {
		try {
			final M message = mailService.getMessage(threads.iterator().next().getLastMessageId());
			new MessageComposer<M, C>(mailService, updateHandler).forward(message);
		} catch (final MailException e) {
			LOGGER.error("forwarding message", e);
		}
	}

	private void reply(final Set<H> threads, final boolean all) {
		try {
			final M message = mailService.getMessage(threads.iterator().next().getLastMessageId());
			new MessageComposer<M, C>(mailService, updateHandler).reply(message, all);
		} catch (final MailException e) {
			LOGGER.error("replying message", e);
		}
	}

	private void star(final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(mailService.findTag(T.STARRED), threads);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("adding {} to thread", T.STARRED, event.getSource().getException()));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void unread(final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(mailService.findTag(T.UNREAD), threads);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("adding {} to thread", T.UNREAD, event.getSource().getException()));
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
		task.setOnFailed(event -> LOGGER.error("archiving thread", event.getSource().getException()));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void trash(final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.trash(threads);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("trashing thread", event.getSource().getException()));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	public void setOnUpdate(final EventHandler<ActionEvent> handler) {
		this.updateHandler = handler;
	}
}
