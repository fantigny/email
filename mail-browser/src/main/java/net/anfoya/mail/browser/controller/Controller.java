package net.anfoya.mail.browser.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.media.AudioClip;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.VoidCallable;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.javafx.scene.section.SectionListPane;
import net.anfoya.tag.model.SpecialTag;

public class Controller<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> {
	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

	private final MailService<S, T, H, M, C> mailService;
	private final UndoService undoService;
	private final Settings settings;

	private final T inbox;
	private final T trash;
	private final T flagged;
	private final T spam;
	private final T unread;

	private MailBrowser<S, T, H, M, C> mailBrowser;
	private SectionListPane<S, T> sectionListPane;
	private ThreadListPane<S, T, H, M, C> threadListPane;
	private final List<ThreadPane<S, T, H, M, C>> threadPanes;

	public Controller(MailService<S, T, H, M, C> mailService, UndoService undoService, Settings settings) {
		this.mailService = mailService;
		this.undoService = undoService;
		this.settings = settings;

		threadPanes = new ArrayList<ThreadPane<S, T, H, M, C>>();

		inbox = mailService.getSpecialTag(SpecialTag.INBOX);
		trash = mailService.getSpecialTag(SpecialTag.TRASH);
		flagged = mailService.getSpecialTag(SpecialTag.FLAGGED);
		spam = mailService.getSpecialTag(SpecialTag.SPAM);
		unread = mailService.getSpecialTag(SpecialTag.UNREAD);
	}

	public void init() {
		sectionListPane.setOnUpdateSection(e -> refreshAfterSectionUpdate());
		sectionListPane.setOnSelectSection(e -> refreshAfterSectionSelect());
		sectionListPane.setOnUpdateTag(e -> refreshAfterTagUpdate());
		sectionListPane.setOnSelectTag(e -> refreshAfterTagSelected());

		threadListPane.setOnArchive(threads -> archive(threads));
		threadListPane.setOnReply(threads -> reply(false, threads));
		threadListPane.setOnReplyAll(threads -> reply(true, threads));
		threadListPane.setOnForward(threads -> forward(threads));
		threadListPane.setOnToggleFlag(threads -> toggleFlag(threads));
		threadListPane.setOnArchive(threads -> archive(threads));
		threadListPane.setOnTrash(threads -> trash(threads));
		threadListPane.setOnToggleSpam(threads -> toggleSpam(threads));

		threadListPane.setOnAddReader(threadPane -> threadPanes.add(threadPane));
		threadListPane.setOnRemoveReader(threadPane -> threadPanes.remove(threadPane));

		threadListPane.setOnLoadThreadList(e -> refreshAfterThreadListLoad());
		threadListPane.setOnSelectThread(e -> refreshAfterThreadSelected());
		threadListPane.setOnUpdatePattern(e -> refreshAfterPatternUpdate());
		threadListPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());

		threadPanes
			.parallelStream()
			.forEach(p -> {
				p.setOnArchive(threads -> archive(threads));
				p.setOnReply(threads -> reply(false, threads));
				p.setOnReplyAll(threads -> reply(true, threads));
				p.setOnForward(threads -> forward(threads));
				p.setOnToggleFlag(threads -> toggleFlag(threads));
				p.setOnArchive(threads -> archive(threads));
				p.setOnTrash(threads -> trash(threads));
				p.setOnToggleSpam(threads -> toggleSpam(threads));
			});
	}

	public void addThreadPane(ThreadPane<S, T, H, M, C> threadPane) {
		threadPanes.add(threadPane);
	}

	public void setSectionListPane(SectionListPane<S, T> sectionListPane) {
		this.sectionListPane = sectionListPane;
	}
	public void setThreadListPane(ThreadListPane<S, T, H, M, C> threadListPane) {
		this.threadListPane = threadListPane;
	}

	private void archive(Set<H> threads) {
		final String description = "archive";
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.archive(threads);
				return null;
			}
		};
		task.setOnSucceeded(e -> {
			undoService.set(() -> mailService.addTagForThreads(inbox, threads), description);
			refreshAfterThreadUpdate();
		});
		task.setOnFailed(e -> LOGGER.error(description, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, description, task);
	}

	private Void reply(final boolean all, Set<H> threads) {
		try {
			for (final H t : threads) {
				final M message = mailService.getMessage(t.getLastMessageId());
				//TODO improve controller
				new MailComposer<M, C>(mailService, e -> refreshAfterThreadUpdate(), settings)
					.reply(message, all);
			}
		} catch (final Exception e) {
			LOGGER.error("load reply{} composer", all ? " all" : "", e);
		}
		return null;
	}

	private void forward(Set<H> threads) {
		try {
			for (final H t : threads) {
				final M message = mailService.getMessage(t.getLastMessageId());
				//TODO improve controller
				new MailComposer<M, C>(mailService, e -> refreshAfterThreadUpdate(), settings)
					.forward(message);
			}
		} catch (final Exception e) {
			LOGGER.error("load forward composer", e);
		}
	}

	private void trash(Set<H> threads) {
		final Iterator<H> iterator = threads.iterator();
		final boolean hasInbox = iterator.hasNext() && iterator.next().getTagIds().contains(inbox.getId());

		new AudioClip(Settings.MP3_TRASH).play();

		final String description = "trash";
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.trash(threads);
				return null;
			}
		};
		task.setOnSucceeded(e -> {
			undoService.set(() -> {
				mailService.removeTagForThreads(trash, threads);
				if (hasInbox) {
					mailService.addTagForThreads(inbox, threads);
				}
			}, description);
			refreshAfterThreadUpdate();
		});
		task.setOnFailed(e -> LOGGER.error(description, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, description, task);
	}

	private void toggleFlag(Set<H> threads) {
		if (threads.isEmpty()) {
			return;
		}
		try {
			if (threads.iterator().next().isFlagged()) {
				removeTagForThreads(flagged, threads, "unflag"
						, () -> addTagForThreads(flagged, threads, "flag", null));
			} else {
				addTagForThreads(flagged, threads, "flag"
						, () -> removeTagForThreads(flagged, threads, "unflag", null));
			}
		} catch (final Exception e) {
			LOGGER.error("toggle flag", e);
		}
	}

	private void toggleSpam(Set<H> threads) {
		if (threads.isEmpty()) {
			return;
		}
		try {
			if (threads.iterator().next().getTagIds().contains(spam.getId())) {
				removeTagForThreads(spam, threads, "not spam", null);
				addTagForThreads(inbox, threads, "not spam", () -> addTagForThreads(spam, threads, "not spam", null));
			} else {
				addTagForThreads(spam, threads, "spam", () -> {
					removeTagForThreads(spam, threads, "spam", null);
					addTagForThreads(inbox, threads, "spam", null);
				});
			}
		} catch (final Exception e) {
			LOGGER.error("toggle flag", e);
		}
	}

	private void addTagForThreads(final T tag, final Set<H> threads, final String desc, final VoidCallable undo) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(tag, threads);
				if (settings.archiveOnDrop().get() && !tag.isSystem()) {
					mailService.archive(threads);
				}
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		task.setOnSucceeded(e -> {
			undoService.set(undo, desc);
			refreshAfterThreadUpdate();
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
	}

	private void removeTagForThreads(final T tag, final Set<H> threads, final String desc, final VoidCallable undo) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.removeTagForThreads(tag, threads);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		task.setOnSucceeded(e -> {
			undoService.set(undo, desc);
			refreshAfterThreadUpdate();
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
	}

	private void createTagForThreads(final String name, final Set<H> threads) {
		final Iterator<H> iterator = threads.iterator();
		final boolean hasInbox = iterator.hasNext() && iterator.next().getTagIds().contains(inbox.getId());
		final String desc = "add " + name;
		final S currentSection = sectionListPane.getSelectedSection();

		final Task<T> task = new Task<T>() {
			@Override
			protected T call() throws Exception {
				final T tag = mailService.addTag(name);
				if (currentSection  != null && !currentSection.isSystem()) {
					mailService.moveToSection(tag, currentSection);
				}
				mailService.addTagForThreads(tag, threads);
				if (settings.archiveOnDrop().get() && hasInbox) {
					mailService.archive(threads);
				}
				return tag;
			}
		};
		task.setOnSucceeded(e -> {
			undoService.set(() -> {
				mailService.remove(task.getValue());
				if (settings.archiveOnDrop().get() && hasInbox) {
					mailService.addTagForThreads(inbox, threads);
				}
			}, desc);
			refreshAfterThreadUpdate();
		});
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
	}


	private final boolean refreshAfterTagSelected = true;
	private final boolean refreshAfterThreadSelected = true;
	private final boolean refreshAfterMoreResultsSelected = true;

	private final boolean refreshAfterThreadListLoad = true;

	private final boolean refreshAfterTagUpdate = true;
	private final boolean refreshAfterSectionUpdate = true;
	private final boolean refreshAfterSectionSelect = true;
	private final boolean refreshAfterThreadUpdate = true;
	private final boolean refreshAfterPatternUpdate = true;
	private final boolean refreshAfterUpdateMessage = true;
	private final boolean refreshAfterUpdateTagOrSection = true;


	private void refreshAfterThreadUpdate() {
		if (!refreshAfterThreadUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterThreadUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	private void refreshAfterThreadListLoad() {
		if (!refreshAfterThreadListLoad) {
			return;
		}
		LOGGER.debug("refreshAfterThreadListLoad");

		final boolean markRead = !sectionListPane.getIncludedOrSelectedTags().contains(unread);
		threadPanes.parallelStream().forEach(p -> {
			if (p.isDetached()) {
				Platform.runLater(() -> p.refresh());
			} else {
				Platform.runLater(() -> p.refresh(threadListPane.getSelectedThreads(), markRead));
			}
		});
//		final String pattern = threadListPane.getNamePattern();
//		if (pattern.isEmpty()) {
			sectionListPane.updateItemCount(threadListPane.getThreadsTags(), threadListPane.getNamePattern(), true);
//		} else {
//			sectionListPane.refreshWithPattern(pattern);
//		}
	}

	private void refreshAfterSectionUpdate() {
		if (!refreshAfterSectionUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterSectionUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	private void refreshAfterSectionSelect() {
		if (!refreshAfterSectionSelect) {
			return;
		}
		LOGGER.debug("refreshAfterSectionSelect");

		threadListPane.setCurrentSection(sectionListPane.getSelectedSection());
	}

	private void refreshAfterTagUpdate() {
		if (!refreshAfterTagUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterTagUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	private void refreshAfterTagSelected() {
		if (!refreshAfterTagSelected) {
			return;
		}
		LOGGER.debug("refreshAfterTagSelected");

		threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
	}

	private void refreshAfterThreadSelected() {
		if (!refreshAfterThreadSelected) {
			return;
		}
		LOGGER.debug("refreshAfterThreadSelected");

		if (!mailBrowser.isFull()) {
			return;
		}

		final Set<H> threads = threadListPane.getSelectedThreads();
		if (threads.size() == 1 && threads.iterator().next() instanceof GmailMoreThreads) {
			refreshAfterMoreThreadsSelected();
			return;
		}

		// update thread details when (a) thread(s) is/are selected
		final boolean markRead = !sectionListPane.getIncludedOrSelectedTags().contains(mailService.getSpecialTag(SpecialTag.UNREAD));
		threadPanes
			.parallelStream()
			.forEach(p -> p.refresh(threadListPane.getSelectedThreads(), markRead));
	}

	private void refreshAfterMoreThreadsSelected() {
		if (!refreshAfterMoreResultsSelected) {
			return;
		}
		LOGGER.debug("refreshAfterMoreResultsSelected");

		// update thread list with next page token
		final GmailMoreThreads more = (GmailMoreThreads) threadListPane.getSelectedThreads().iterator().next();
		threadListPane.refreshWithPage(more.getPage());
	}

	private void refreshAfterPatternUpdate() {
		if (!refreshAfterPatternUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterPatternUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	public void setMailBrowser(MailBrowser<S, T, H, M, C> mailBrowser) {
		this.mailBrowser = mailBrowser;
	}

}
