package net.anfoya.mail.browser.javafx.threadlist;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.dnd.DropArea;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.SpecialTag;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadListDropPane<T extends Tag, H extends Thread, M extends Message, C extends Contact> extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadListDropPane.class);

	private final MailService<? extends Section, T, H, M, C> mailService;
	private EventHandler<ActionEvent> updateHandler;

	private final T flaggedTag;
	private final T unreadTag;

	public ThreadListDropPane(final MailService<? extends Section, T, H, M, C> mailService) throws MailException {
		this.mailService = mailService;

		setMaxHeight(180);

		flaggedTag = mailService.getSpecialTag(SpecialTag.FLAGGED);
		unreadTag = mailService.getSpecialTag(SpecialTag.UNREAD);

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

		final DropArea flagArea = new DropArea("flag", DND_THREADS_DATA_FORMAT);
		flagArea.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				flag(threads);
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
		addRow(i++, flagArea);
		addRow(i++, archiveArea, trashArea);
		addRow(i++, replyArea, replyAllArea);
		addRow(i++, forwardArea, unreadArea);

		setHgrow(flagArea, Priority.ALWAYS);
		setVgrow(flagArea, Priority.ALWAYS);
		setHgrow(archiveArea, Priority.ALWAYS);
		setVgrow(archiveArea, Priority.ALWAYS);
		setHgrow(trashArea, Priority.ALWAYS);
		setVgrow(trashArea, Priority.ALWAYS);
		setHgrow(replyArea, Priority.ALWAYS);
		setVgrow(replyArea, Priority.ALWAYS);
		setHgrow(replyAllArea, Priority.ALWAYS);
		setVgrow(replyAllArea, Priority.ALWAYS);
		setHgrow(forwardArea, Priority.ALWAYS);
		setVgrow(forwardArea, Priority.ALWAYS);
		setHgrow(unreadArea, Priority.ALWAYS);
		setVgrow(unreadArea, Priority.ALWAYS);

	}

	private void forward(final Set<H> threads) {
		try {
			final M message = mailService.getMessage(threads.iterator().next().getLastMessageId());
			new MailComposer<M, C>(mailService, updateHandler).forward(message);
		} catch (final MailException e) {
			LOGGER.error("forwarding message", e);
		}
	}

	private void reply(final Set<H> threads, final boolean all) {
		try {
			final M message = mailService.getMessage(threads.iterator().next().getLastMessageId());
			new MailComposer<M, C>(mailService, updateHandler).reply(message, all);
		} catch (final MailException e) {
			LOGGER.error("replying message", e);
		}
	}

	private void flag(final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(flaggedTag, threads);
				return null;
			}
		};
		task.setOnSucceeded(e -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("adding {} to thread", flaggedTag, e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void unread(final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(unreadTag, threads);
				return null;
			}
		};
		task.setOnSucceeded(e -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("adding {} to thread", unreadTag, e.getSource().getException()));
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
		task.setOnSucceeded(e -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("archiving thread", e.getSource().getException()));
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
		task.setOnSucceeded(e -> updateHandler.handle(null));
		task.setOnFailed(e -> LOGGER.error("trashing thread", e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(task);
	}

	public void setOnUpdate(final EventHandler<ActionEvent> handler) {
		this.updateHandler = handler;
	}
}
