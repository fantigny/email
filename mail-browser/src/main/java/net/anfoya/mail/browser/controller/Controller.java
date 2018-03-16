
package net.anfoya.mail.browser.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.media.AudioClip;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.VoidCallable;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.javafx.notification.NotificationService;
import net.anfoya.mail.browser.controller.vo.TagForThreadsVo;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.MailBrowser.Mode;
import net.anfoya.mail.browser.javafx.message.MailReader;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.settings.SettingsDialog;
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
	private final T unread;
	private final T draft;

	private MailBrowser<S, T, H, M, C> mailBrowser;
	private SectionListPane<S, T> sectionListPane;
	private ThreadListPane<T, H> threadListPane;
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
		unread = mailService.getSpecialTag(SpecialTag.UNREAD);
		draft = mailService.getSpecialTag(SpecialTag.DRAFT);
	}

	public void init() {
		mailService.addOnUpdateMessage(() -> Platform.runLater(() -> refreshAfterUpdateMessage()));
//		mailService.addOnUpdateTagOrSection(() -> Platform.runLater(() -> refreshTags()));
		mailService.disconnectedProperty().addListener((ov, o, n) -> {
			if (!o && n) {
				Platform.runLater(() -> refreshAfterTagSelected());
			}
		});

		String section = settings.sectionName().get();
		String tag = settings.tagName().get();
		if (section.isEmpty() || tag.isEmpty()) {
			section = GmailSection.SYSTEM.getName();
			tag = mailService.getSpecialTag(SpecialTag.INBOX).getName();
		}
		sectionListPane.init(section, tag);
	}

	public void setSectionListPane(SectionListPane<S, T> pane) {
		sectionListPane = pane;
		sectionListPane.setOnUpdateSection(e -> refreshAfterSectionUpdate());
		sectionListPane.setOnSelectSection(e -> refreshAfterSectionSelect());
		sectionListPane.setOnUpdateTag(e -> refreshAfterTagUpdate());
		sectionListPane.setOnSelectTag(e -> refreshAfterTagSelected());
	}

	public void setThreadListPane(ThreadListPane<T, H> pane) {
		threadListPane = pane;

		threadListPane.setOnLoad(() -> refreshAfterThreadListLoad());
		threadListPane.setOnUpdatePattern(() -> refreshAfterPatternUpdate());

		threadListPane.setOnCompose(() -> compose(""));
		threadListPane.setOnArchive(threads -> archive(threads));
		threadListPane.setOnReply(threads -> reply(threads, false));
		threadListPane.setOnReplyAll(threads -> reply(threads, true));
		threadListPane.setOnForward(threads -> forward(threads));
		threadListPane.setOnTrash(threads -> trash(threads));

		threadListPane.setOnView(threads -> view(threads));
		threadListPane.setOnOpen(threads -> open(threads));

		threadListPane.setOnToggleFlag(threads -> toggleFlag(threads));
		threadListPane.setOnToggleSpam(threads -> toggleSpam(threads));
		threadListPane.setOnAddTagForThreads(vo -> addTagForThreads(vo));
		threadListPane.setOnCreateTagForThreads(vo -> createTagForThreads(vo.getTag().getName(), vo.getThreads()));

		threadListPane.disconnectedProperty().bind(mailService.disconnectedProperty());
		threadListPane.setOnReconnect(() -> reconnect());

		threadListPane.setOnShowSettings(() -> showSettingsDialog());
		threadListPane.setOnSignout(() -> signout());

		threadListPane.showToolBarProperty().bind(settings.showToolbar());
	}

	public void addThreadPane(ThreadPane<S, T, H, M, C> pane) {
		threadPanes.add(pane);

		pane.setOnArchive(tSet -> archive(tSet));
		pane.setOnReply(tSet -> reply(tSet, false));
		pane.setOnReplyAll(tSet -> reply(tSet, false));
		pane.setOnForward(tSet -> forward(tSet));
		pane.setOnToggleFlag(tSet -> toggleFlag(tSet));
		pane.setOnArchive(tSet -> archive(tSet));
		pane.setOnTrash(tSet -> trash(tSet));
		pane.setOnToggleSpam(tSet -> toggleSpam(tSet));
		pane.setOnOpenUrl(url -> openUrl(url));
		pane.setOnMarkRead(tSet -> markUnread(tSet));
		pane.setOnRemoveTagForThreads(vo -> removeTagForThreads(vo));
		pane.setOnShowSettings(() -> showSettingsDialog());
		pane.setOnSignout(() -> signout());
	}

	public void setMailBrowser(MailBrowser<S, T, H, M, C> mailBrowser) {
		this.mailBrowser = mailBrowser;
		this.mailBrowser.addOnModeChange(() -> view(threadListPane.getSelectedThreads()));
	}

	private void openUrl(String url) {
		UrlHelper.open(url, r -> compose(r));
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
			undoService.setUndo(() -> {
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

	private void open(Set<H> threads) {
		final boolean draftList = includes.contains(draft);
		threads.forEach(t -> {
			if (draftList) {
				edit(t);
			} else {
				open(t);
			}
		});
	}

	private void open(H thread) {
		final ThreadPane<S, T, H, M, C> pane = createDetachedThreadPane(thread);

		final MailReader mailReader = new MailReader(pane);
		mailReader.setOnHidden(ev -> threadPanes.remove(pane));
		mailReader.show();
	}

	private ThreadPane<S, T, H, M, C> createDetachedThreadPane(H thread) {
		final ThreadPane<S, T, H, M, C> pane = new ThreadPane<>(mailService, undoService, settings);
		pane.setDetached(true);
		pane.refresh(Collections.singleton(thread));

		addThreadPane(pane);

		return pane;
	}

	private void view(final Set<H> threads) {
		if (mailBrowser.modeProperty().get() != Mode.FULL) {
			return;
		}

		if (threads.size() == 1 && threads.iterator().next() instanceof GmailMoreThreads) {
			refreshAfterMoreThreadsSelected();
		} else {
			threadPanes.get(0).refresh(threads);
		}
	}

	private void refreshTags() {
		threadPanes
			.stream()
			.filter(p -> p.getThread() != null)
			.forEach(p -> {
				final H thread = p.getThread();
				final Task<Set<H>> task = new Task<Set<H>>() {
					@Override protected Set<H> call() throws Exception {
						try {
							return Collections.singleton(mailService.getThread(thread.getId()));
						} catch (final MailException e) {
							LOGGER.error("load thread {} {}", thread.getId(), thread.getSubject());
							return null;
						}
					}
				};
				task.setOnFailed(e -> LOGGER.error("reload thread {}", thread.getId(), e.getSource().getException()));
				task.setOnSucceeded(e -> {
					Platform.runLater(() -> p.refresh(task.getValue()));
				});
				ThreadPool.getDefault().submit(PoolPriority.MAX, "reload tags", task);

			});
	}

	private void markUnread(Set<H> threads) {
		try {
			removeTagForThreads(unread, threads, "mark unread", (VoidCallable)null);
		} catch (final Exception e) {
			LOGGER.error("mark unread", e);
		}
	}

	private void archive(final Set<H> threads) {
		try {
			removeTagForThreads(inbox, threads, "archive", "unarchive");
		} catch (final Exception e) {
			LOGGER.error("archive", e);
		}
	}


	private void toggleFlag(Set<H> threads) {
		if (threads.isEmpty()) {
			return;
		}
		try {
			if (threads.iterator().next().isFlagged()) {
				removeTagForThreads(flagged, threads, "unflag", "flag");
			} else {
				addTagForThreads(flagged, threads, "flag", "unflag");
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
			if (threads.iterator().next().isSpam()) {
				unsetSpam(threads);
			} else {
				setSpam(threads);
			}
		} catch (final Exception e) {
			LOGGER.error("toggle spam", e);
		}
	}

	private void setSpam(Set<H> threads) {
		addTagForThreads(spam, threads, "spam", () -> unsetSpam(threads));
	}

	private void unsetSpam(Set<H> threads) {
		removeTagForThreads(spam, threads, "not spam", () -> setSpam(threads));
		addTagForThreads(inbox, threads, "not spam", (VoidCallable)null);
	}

	private void addTagForThreads(TagForThreadsVo<T, H> vo) {
		addTagForThreads(vo.getTag(), vo.getThreads());
	}

	private void addTagForThreads(final T tag, final Set<H> threads) {
		addTagForThreads(tag, threads, "add " + tag.getName(), "remove " + tag.getName());
	}

	private void addTagForThreads(final T tag, final Set<H> threads, String desc, String undoDesc) {
		addTagForThreads(tag, threads, desc, () -> removeTagForThreads(tag, threads, undoDesc, desc));
	}

	private void addTagForThreads(final T tag, final Set<H> threads, String desc, VoidCallable undo) {
		if (threads.isEmpty()) {
			return;
		}
		final Task<Void> task = new Task<Void>() {
			@Override protected Void call() throws Exception {
				mailService.addTagForThreads(tag, threads);
				if (settings.archiveOnDrop().get() && !tag.isSystem()) {
					mailService.archive(threads);
				}
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		task.setOnSucceeded(e -> {
			if (undo != null) {
				undoService.setUndo(undo, desc);
			}
			refreshTags();
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
	}

	private void removeTagForThreads(TagForThreadsVo<T, H> vo) {
		removeTagForThreads(vo.getTag(), vo.getThreads());
	}

	private void removeTagForThreads(final T tag, final Set<H> threads) {
		removeTagForThreads(tag, threads, "remove " + tag.getName(), "add " + tag.getName());
	}

	private void removeTagForThreads(final T tag, final Set<H> threads, String desc, String undoDesc) {
		removeTagForThreads(tag, threads, desc, () -> addTagForThreads(tag, threads, undoDesc, desc));
	}

	private void removeTagForThreads(final T tag, final Set<H> threads, final String desc, VoidCallable undo) {
		if (threads.isEmpty()) {
			return;
		}
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.removeTagForThreads(tag, threads);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		task.setOnSucceeded(e -> {
			if (undo != null && desc != null) {
				undoService.setUndo(undo, desc);
			}
			refreshTags();
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
	}

	private void createTagForThreads(final String name, final Set<H> threads) {
		final Iterator<H> iterator = threads.iterator();
		final boolean hasInbox = iterator.hasNext() && iterator.next().getTagIds().contains(inbox.getId());
		final String desc = "add " + name;

		final Task<T> task = new Task<T>() {
			@Override protected T call() throws Exception {
				final T tag = mailService.addTag(name);
				if (currentSection != null && !currentSection.isSystem()) {
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
			final T tag = task.getValue();
			undoService.setUndo(() -> {
				mailService.remove(tag);
				if (hasInbox && settings.archiveOnDrop().get()) {
					mailService.addTagForThreads(inbox, threads);
				}
			}, desc);
			refreshTags();
		});
		task.setOnFailed(e -> LOGGER.error(desc, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, desc, task);
	}

	private final boolean refreshUnreadCount = true;
	private final boolean refreshAfterTagSelected = true;
	private final boolean refreshAfterMoreResultsSelected = true;

	private final boolean refreshAfterThreadListLoad = true;

	private final boolean refreshAfterTagUpdate = true;
	private final boolean refreshAfterSectionUpdate = true;
	private final boolean refreshAfterSectionSelect = true;
	private final boolean refreshAfterThreadUpdate = true;
	private final boolean refreshAfterPatternUpdate = true;
	private final boolean refreshAfterUpdateMessage = true;

	private S currentSection;

	private void refreshAfterThreadUpdate() {
		if (!refreshAfterThreadUpdate) {
			LOGGER.warn("refreshAfterThreadUpdate");
			return;
		}
		LOGGER.debug("refreshAfterThreadUpdate");

		sectionListPane.refreshAsync(() -> loadThreadList(true));
	}

	private void refreshAfterThreadListLoad() {
		if (!refreshAfterThreadListLoad) {
			LOGGER.warn("refreshAfterThreadListLoad");
			return;
		}
		LOGGER.debug("refreshAfterThreadListLoad");

		//TODO review if necessary, maybe not worth fixing few inconsistencies in item counts
		final Set<T> tags = threadListPane.getThreadsTagIds()
				.stream()
				.map(id -> {
					try {
						return mailService.getTag(id);
					} catch (final MailException e) {
						LOGGER.error("get tag {}", id, e);
						return null;
					}
				})
				.collect(Collectors.toSet());


		sectionListPane.updateItemCount(tags, threadListPane.getSearchPattern(), true);
	}

	private void refreshAfterSectionSelect() {
		if (!refreshAfterSectionSelect) {
			LOGGER.warn("refreshAfterSectionSelect");
			return;
		}
		LOGGER.debug("refreshAfterSectionSelect");

		currentSection = sectionListPane.getSelectedSection();
	}

	private void refreshAfterTagUpdate() {
		if (!refreshAfterTagUpdate) {
			LOGGER.warn("refreshAfterTagUpdate");
			return;
		}
		LOGGER.debug("refreshAfterTagUpdate");

		sectionListPane.refreshAsync(() -> loadThreadList(true));
	}

	private void refreshAfterTagSelected() {
		if (!refreshAfterTagSelected) {
			LOGGER.warn("refreshAfterTagSelected");
			return;
		}
		LOGGER.debug("refreshAfterTagSelected");

		loadThreadList(false);
	}

	private void refreshAfterMoreThreadsSelected() {
		if (!refreshAfterMoreResultsSelected) {
			LOGGER.warn("refreshAfterMoreResultsSelected");
			return;
		}
		LOGGER.debug("refreshAfterMoreResultsSelected");

		loadThreadListNextSet();
	}

	private void refreshAfterPatternUpdate() {
		if (!refreshAfterPatternUpdate) {
			LOGGER.warn("refreshAfterPatternUpdate");
			return;
		}
		LOGGER.debug("refreshAfterPatternUpdate");

		sectionListPane.refreshAsync(() -> loadThreadList(true));
	}

	private void refreshAfterUpdateMessage() {
		if (!refreshAfterUpdateMessage) {
			LOGGER.warn("refreshAfterUpdateMessage");
			return;
		}
		LOGGER.debug("refreshAfterUpdateMessage");
		LOGGER.info("message update detected");

		refreshTags();
		refreshUnreadCount();
		sectionListPane.refreshAsync(() -> loadThreadList(true));
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

	private void refreshAfterSectionUpdate() {
		if (!refreshAfterSectionUpdate) {
			LOGGER.warn("refreshAfterSectionUpdate");
			return;
		}
		LOGGER.debug("refreshAfterSectionUpdate");

		sectionListPane.refreshAsync(() -> loadThreadList(true));
	}


	/////// compose

	private MailComposer<M, C> createComposer() {
		final MailComposer<M, C> composer = new MailComposer<>(mailService, settings);
		composer.setOnSend(d -> send(d));
		composer.setOnDiscard(d -> discard(d));
		composer.setOnCompose(r -> compose(r));

		return composer;
	}

	public void compose(final String recipient) {
		LOGGER.debug("compose");
		final MailComposer<M, C> composer = createComposer();
		final Task<M> task = new Task<M>() {
			@Override protected M call() throws Exception {
				return mailService.createDraft(null);
			}
		};
		task.setOnFailed(e -> LOGGER.error("new draft", e.getSource().getException()));
		task.setOnSucceeded(e -> composer.compose(task.getValue(), recipient));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "new draft", task);
	}

	private void edit(H thread) {
		LOGGER.debug("edit draft");
		final MailComposer<M, C> composer = createComposer();
		final Task<M> task = new Task<M>() {
			@Override protected M call() throws Exception {
				return mailService.getDraft(thread.getId());
			}
		};
		task.setOnFailed(e -> LOGGER.error("read draft {}", thread.getId(), e.getSource().getException()));
		task.setOnSucceeded(e -> composer.edit(task.getValue()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "read draft", task);
	}

	private void reply(Set<H> threads, final boolean all) {
		LOGGER.debug("reply");
		threads.forEach(t -> {
			final MailComposer<M, C> composer = createComposer();
			final Task<List<M>> task = new Task<List<M>>() {
				@Override protected List<M> call() throws Exception {
					final List<M> messages = new ArrayList<>();
					messages.add(mailService.getMessage(t.getLastMessageId()));
					messages.add(0, mailService.createDraft(messages.get(0)));
					return messages;
				}
			};
			task.setOnFailed(event -> LOGGER.error("create reply draft", event.getSource().getException()));
			task.setOnSucceeded(e -> composer.reply(task.getValue().get(0), task.getValue().get(1), all));
			ThreadPool.getDefault().submit(PoolPriority.MAX, "create reply draft", task);
		});
	}

	private void forward(Set<H> threads) {
		LOGGER.debug("forward");
		threads.forEach(t -> {
			final MailComposer<M, C> composer = createComposer();
			final Task<List<M>> task = new Task<List<M>>() {
				@Override protected List<M> call() throws Exception {
					final List<M> messages = new ArrayList<>();
					messages.add(mailService.getMessage(t.getLastMessageId()));
					messages.add(0, mailService.createDraft(messages.get(0)));
					return messages;
				}
			};
			task.setOnFailed(event -> LOGGER.error("create reply draft", event.getSource().getException()));
			task.setOnSucceeded(e -> composer.forward(task.getValue().get(0), task.getValue().get(1)));
			ThreadPool.getDefault().submit(PoolPriority.MAX, "create reply draft", task);
		});
	}

	private void send(M draft) {
		LOGGER.debug("send");
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

	/////////////// section pane
	private final Set<T> includes = new HashSet<>();
	private final Set<T> excludes = new HashSet<>();


	/////////////// thread list
	private final AtomicBoolean isUnreadList = new AtomicBoolean();

	private final StringProperty pattern = new SimpleStringProperty("");
	private final AtomicInteger threadListPage = new AtomicInteger(1);

	private final AtomicLong loadThreadsTaskId = new AtomicLong();
	private Task<Set<H>> loadThreadsTask = null;

	private final Set<H> threads = new HashSet<>();

	private SettingsDialog<S, T> settingsDialog;

	public void loadThreadListNextSet() {
		threadListPage.incrementAndGet();
		loadThreadList(true);
	}

	public void loadThreadList(final boolean update) {
		final Set<T> includes = sectionListPane.getIncludedOrSelectedTags();
		final Set<T> excludes = sectionListPane.getExcludedTags();
		final String pattern = threadListPane.getSearchPattern();

		// check if new filter and new page
		final boolean newFilter = !includes.equals(this.includes) || !excludes.equals(this.excludes) || !pattern.equals(this.pattern.get());
		if (!newFilter && !update) {
			return;
		}

		isUnreadList.set(includes.size() == 1 && includes.iterator().next().getId().equals(unread.getId()));

		final int page = newFilter? 1: threadListPage.get();
		threadListPage.set(page);

		synchronized (includes) {
			this.includes.clear();
			this.includes.addAll(includes);
		}
		synchronized (excludes) {
			this.excludes.clear();
			this.excludes.addAll(excludes);
		}

		this.pattern.set(pattern);

		final long taskId = loadThreadsTaskId.incrementAndGet();
		if (loadThreadsTask != null && loadThreadsTask.isRunning()) {
			loadThreadsTask.cancel();
		}

		final Set<H> previousThreads = this.threads;

		loadThreadsTask = new Task<Set<H>>() {
			@Override
			protected Set<H> call() throws InterruptedException, MailException {
				LOGGER.debug("load for includes {}, excludes {}, pattern: {}, pageMax: {}", includes, excludes, pattern, page);
				final Set <H> threads = mailService.findThreads(includes, excludes, pattern, threadListPage.get());
				// if unread list we add the older items even if they are read now
				if (isUnreadList.get() && !newFilter) {
					threads.addAll(previousThreads
							.stream()
							.filter(t -> !threads.contains(t))
							.map(t -> {
								try {
									return mailService.getThread(t.getId());
								} catch (final MailException e) {
									LOGGER.error("load thread {}", t.getSubject(), e);
									return null;
								}
							})
							.collect(Collectors.toSet()));
				}
				return threads;
			}
		};
		loadThreadsTask.setOnFailed(e -> LOGGER.error("load thread list", e.getSource().getException()));
		loadThreadsTask.setOnSucceeded(e -> {
			if (taskId == loadThreadsTaskId.get()) {
				final Set<H> threads = loadThreadsTask.getValue();
				synchronized (this.threads) {
					this.threads.clear();
					this.threads.addAll(threads);
				}
				Platform.runLater(() -> threadListPane.setAll(threads));
			}
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, "load thread list", loadThreadsTask);
	}


	///////////////////// system

	private void reconnect() {
		try {
			mailService.reconnect();
		} catch (final Exception e) {
			LOGGER.error("reconnect", e);
		}
	}

	private void showSettingsDialog() {
		if (settingsDialog == null
				|| !settingsDialog.isShowing()) {
			settingsDialog = new SettingsDialog<>(mailService, undoService, settings);
			settingsDialog.show();
		}
		settingsDialog.toFront();
		settingsDialog.requestFocus();
	}

	private void signout() {
		mailBrowser.signout();
	}
}
