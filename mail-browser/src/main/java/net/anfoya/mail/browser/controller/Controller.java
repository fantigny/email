package net.anfoya.mail.browser.controller;

import java.util.ArrayList;
import java.util.Collections;
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
import net.anfoya.javafx.notification.NotificationService;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.message.MailReader;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.browser.javafx.util.UrlHelper;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
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
	private final NotificationService notificationService;
	private final UndoService undoService;
	private final Settings settings;

	private final T inbox;
	private final T trash;
	private final T flagged;
	private final T spam;
	private final T draft;

	private MailBrowser<S, T, H, M, C> mailBrowser;
	private SectionListPane<S, T> sectionListPane;
	private ThreadListPane<S, T, H, M, C> threadListPane;
	private final List<ThreadPane<S, T, H, M, C>> threadPanes;

	public Controller(MailService<S, T, H, M, C> mailService
			, NotificationService notificationService
			, UndoService undoService
			, Settings settings) {
		this.mailService = mailService;
		this.notificationService = notificationService;
		this.undoService = undoService;
		this.settings = settings;

		threadPanes = new ArrayList<>();

		inbox = mailService.getSpecialTag(SpecialTag.INBOX);
		trash = mailService.getSpecialTag(SpecialTag.TRASH);
		flagged = mailService.getSpecialTag(SpecialTag.FLAGGED);
		spam = mailService.getSpecialTag(SpecialTag.SPAM);
		draft = mailService.getSpecialTag(SpecialTag.DRAFT);
	}

	public void init() {
		mailService.addOnUpdateMessage(() -> Platform.runLater(() -> refreshAfterUpdateMessage()));
		mailService.addOnUpdateTagOrSection(() -> Platform.runLater(() -> refreshAfterUpdateTagOrSection()));
		mailService.disconnectedProperty().addListener((ov, o, n) -> {
			if (!o && n) {
				Platform.runLater(() -> refreshAfterTagSelected());
			}
		});

		sectionListPane.setOnUpdateSection(e -> refreshAfterSectionUpdate());
		sectionListPane.setOnSelectSection(e -> refreshAfterSectionSelect());
		sectionListPane.setOnUpdateTag(e -> refreshAfterTagUpdate());
		sectionListPane.setOnSelectTag(e -> refreshAfterTagSelected());

		threadListPane.setOnArchive(threads -> archive(threads));
		threadListPane.setOnReply(threads -> reply(threads, false));
		threadListPane.setOnReplyAll(threads -> reply(threads, true));
		threadListPane.setOnForward(threads -> forward(threads));
		threadListPane.setOnToggleFlag(threads -> toggleFlag(threads));
		threadListPane.setOnTrash(threads -> trash(threads));
		threadListPane.setOnToggleSpam(threads -> toggleSpam(threads));

		threadListPane.setOnView(threads -> view(threads));
		threadListPane.setOnOpen(threads -> open(threads));
		threadListPane.setOnTagUpdate(t -> sectionListPane.refreshAsync(null));
		
		threadListPane.setOnLoad(() -> refreshAfterThreadListLoad());
		threadListPane.setOnUpdatePattern(() -> refreshAfterPatternUpdate());

		threadPanes
			.stream()
			.forEach(p -> {
				p.setOnArchive(threads -> archive(threads));
				p.setOnReply(threads -> reply(threads, false));
				p.setOnReplyAll(threads -> reply(threads, true));
				p.setOnForward(threads -> forward(threads));
				p.setOnToggleFlag(threads -> toggleFlag(threads));
				p.setOnTrash(threads -> trash(threads));
				p.setOnToggleSpam(threads -> toggleSpam(threads));
				p.setOnOpenUrl(url -> openUrl(url));
			});

		mailBrowser.addOnModeChange(() -> refreshAfterModeChange());

		String section = settings.sectionName().get();
		String tag = settings.tagName().get();
		if (section.isEmpty() || tag.isEmpty()) {
			section = GmailSection.SYSTEM.getName();
			tag = mailService.getSpecialTag(SpecialTag.INBOX).getName();
		}
		sectionListPane.init(section, tag);
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

	private void reply(Set<H> threads, final boolean all) {
		try {
			for (final H t : threads) {
				final M message = mailService.getMessage(t.getLastMessageId());
				final MailComposer<M, C> composer = new MailComposer<M, C>(mailService, settings);
				composer.setOnSend(d -> send(d));
				composer.setOnDiscard(d -> discard(d));
				composer.reply(message, all);
			}
		} catch (final Exception e) {
			LOGGER.error("load reply{} composer", all ? " all" : "", e);
		}
	}

	private void forward(Set<H> threads) {
		try {
			for (final H t : threads) {
				final M message = mailService.getMessage(t.getLastMessageId());
				final MailComposer<M, C> composer = new MailComposer<M, C>(mailService, settings);
				composer.setOnSend(d -> send(d));
				composer.setOnDiscard(d -> discard(d));
				composer.forward(message);
			}
		} catch (final Exception e) {
			LOGGER.error("load forward composer", e);
		}
	}

	private void openUrl(String url) {
		UrlHelper.open(url, recipient -> {
			try {
				final MailComposer<M, C> composer = new MailComposer<M, C>(mailService, settings);
				composer.setOnSend(d -> send(d));
				composer.setOnDiscard(d -> discard(d));
				composer.setOnOpenUrl(u -> openUrl(u));
				composer.newMessage(recipient);
			} catch (final MailException e) {
				LOGGER.error("create new mail to {}", recipient, e);
			}
			return null;
		});
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

	private void send(M draft) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.send(draft);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("send message", e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MIN, "send message", task);
	}

	private void discard(M draft) {
		LOGGER.debug("discard draft");
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.remove(draft);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("delete draft {}", draft, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MIN, "delete draft", task);
	}

	private void open(Set<H> threads) {
		boolean isDraft = sectionListPane.getIncludedOrSelectedTags().contains(draft);
		if (isDraft) {
			openDraft(threads);
		} else {
			openThread(threads);
		}
	}

	private void openDraft(Set<H> threads) {
		threads.forEach(t -> {
			final MailComposer<M, C> composer = new MailComposer<M, C>(mailService, settings);
			composer.setOnSend(d -> send(d));
			composer.setOnDiscard(d -> discard(d));
			composer.setOnOpenUrl(u -> openUrl(u));
			composer.editOrReply(t.getLastMessageId(), false);
		});
	}
	
	private void openThread(Set<H> threads) {
		threads.forEach(t -> {
			final ThreadPane<S, T, H, M, C> pane = new ThreadPane<S, T, H, M, C>(mailService, undoService, settings);
			threadPanes.add(pane);
			pane.setDetached(true);
			pane.setOnArchive(tSet -> archive(tSet));
			pane.setOnReply(tSet -> reply(tSet, false));
			pane.setOnReplyAll(tSet -> reply(tSet, false));
			pane.setOnForward(tSet -> forward(tSet));
			pane.setOnToggleFlag(tSet -> toggleFlag(tSet));
			pane.setOnArchive(tSet -> archive(tSet));
			pane.setOnTrash(tSet -> trash(tSet));
			pane.setOnToggleSpam(tSet -> toggleSpam(tSet));
			pane.refresh(Collections.singleton(t));
			pane.setOnOpenUrl(url -> openUrl(url));

			final MailReader mailReader = new MailReader(pane);
			mailReader.show();
			mailReader.setOnHidden(ev -> threadPanes.remove(pane));			
		});
	}
	
	private void view(Set<H> threads) {
		if (threads.size() == 1 && threads.iterator().next() instanceof GmailMoreThreads) {
			refreshAfterMoreThreadsSelected();
		} else {
			// update thread details when threads are selected
			threadPanes
				.parallelStream()
				.forEach(p -> Platform.runLater(() -> p.refresh(threads)));
		}
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

	private boolean refreshUnreadCount = true;

	private boolean refreshAfterTagSelected = true;
	private boolean refreshAfterMoreResultsSelected = true;

	private final boolean refreshAfterThreadListLoad = true;

	private final boolean refreshAfterTagUpdate = true;
	private final boolean refreshAfterSectionUpdate = true;
	private final boolean refreshAfterSectionSelect = true;
	private final boolean refreshAfterThreadUpdate = true;
	private final boolean refreshAfterPatternUpdate = true;
	private final boolean refreshAfterUpdateMessage = true;
	private final boolean refreshAfterUpdateTagOrSection = true;
	private final boolean refreshAfterModeChange = true;

	
	private void refreshAfterThreadUpdate() {
		if (!refreshAfterThreadUpdate) {
			LOGGER.warn("refreshAfterThreadUpdate");
			return;
		}
		LOGGER.debug("refreshAfterThreadUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	private void refreshAfterThreadListLoad() {
		if (!refreshAfterThreadListLoad) {
			LOGGER.warn("refreshAfterThreadListLoad");
			return;
		}
		LOGGER.debug("refreshAfterThreadListLoad");

		//TODO review if necessary, maybe not worth fixing few inconsistencies in item counts
		sectionListPane.updateItemCount(threadListPane.getThreadsTags(), threadListPane.getNamePattern(), true);
	}

	private void refreshAfterSectionSelect() {
		if (!refreshAfterSectionSelect) {
			LOGGER.warn("refreshAfterSectionSelect");
			return;
		}
		LOGGER.debug("refreshAfterSectionSelect");

		threadListPane.setCurrentSection(sectionListPane.getSelectedSection());
	}

	private void refreshAfterTagUpdate() {
		if (!refreshAfterTagUpdate) {
			LOGGER.warn("refreshAfterTagUpdate");
			return;
		}
		LOGGER.debug("refreshAfterTagUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	private void refreshAfterTagSelected() {
		if (!refreshAfterTagSelected) {
			LOGGER.warn("refreshAfterTagSelected");
			return;
		}
		LOGGER.debug("refreshAfterTagSelected");

		threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
	}

	private void refreshAfterMoreThreadsSelected() {
		if (!refreshAfterMoreResultsSelected) {
			LOGGER.warn("refreshAfterMoreResultsSelected");
			return;
		}
		LOGGER.debug("refreshAfterMoreResultsSelected");

		// update thread list with next page token
		final GmailMoreThreads more = (GmailMoreThreads) threadListPane.getSelectedThreads().iterator().next();
		threadListPane.refreshWithPage(more.getPage());
	}

	private void refreshAfterPatternUpdate() {
		if (!refreshAfterPatternUpdate) {
			LOGGER.warn("refreshAfterPatternUpdate");
			return;
		}
		LOGGER.debug("refreshAfterPatternUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	public void setMailBrowser(MailBrowser<S, T, H, M, C> mailBrowser) {
		this.mailBrowser = mailBrowser;
	}

	private void refreshAfterUpdateMessage() {
		if (!refreshAfterUpdateMessage) {
			LOGGER.warn("refreshAfterUpdateMessage");
			return;
		}
		LOGGER.debug("refreshAfterUpdateMessage");
		LOGGER.info("message update detected");

		refreshUnreadCount();
		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	private void refreshUnreadCount() {
		if (!refreshUnreadCount) {
			LOGGER.warn("refreshUnreadCount");
			return;
		}
		LOGGER.debug("refreshUnreadCount");

		final String desc = "count unread messages";
		final Set<T> includes = Collections.singleton(mailService.getSpecialTag(SpecialTag.UNREAD));
		ThreadPool.getDefault().submit(PoolPriority.MIN, desc, () -> {
			int count = 0;
			try {
				count = mailService.findThreads(includes, Collections.emptySet(), "", 200).size();
			} catch (final MailException e) {
				LOGGER.error(desc, e);
			}
			notificationService.setIconBadge("" + (count > 0? count: ""));
		});
	}

	private void refreshAfterUpdateTagOrSection() {
		if (!refreshAfterUpdateTagOrSection) {
			LOGGER.warn("refreshAfterUpdateTagOrSection");
			return;
		}
		LOGGER.debug("refreshAfterUpdateTagOrSection");
		LOGGER.info("label update detected");

		refreshAfterSectionUpdate();
	}
	private void refreshAfterSectionUpdate() {
		if (!refreshAfterSectionUpdate) {
			LOGGER.warn("refreshAfterSectionUpdate");
			return;
		}
		LOGGER.debug("refreshAfterSectionUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}


	private void refreshAfterModeChange() {
		if (!refreshAfterModeChange) {
			LOGGER.warn("refreshAfterModeChange");
			return;
		}
		LOGGER.debug("refreshAfterModeChange");
	}
}
