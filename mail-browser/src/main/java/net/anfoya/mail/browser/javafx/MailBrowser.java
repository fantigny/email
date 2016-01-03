package net.anfoya.mail.browser.javafx;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.ThreadPriority;
import net.anfoya.mail.browser.javafx.css.StyleHelper;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
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

public class MailBrowser<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends Scene {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowser.class);

	private final MailService<S, T, H, M, C> mailService;
	private final NotificationService notificationService;

	private final SplitPane splitPane;
	private final SectionListPane<S, T> sectionListPane;
	private final ThreadListPane<S, T, H, M, C> threadListPane;
	private final ThreadPane<S, T, H, M, C> threadPane;

	public MailBrowser(final MailService<S, T, H, M, C> mailService
			, final NotificationService notificationService
			, final Settings settings) throws MailException {
		super(new SplitPane(), Color.TRANSPARENT);
		this.mailService = mailService;
		this.notificationService = notificationService;

		StyleHelper.addCommonCss(this);
		StyleHelper.addCss(this, "/net/anfoya/javafx/scene/control/excludebox.css");

		splitPane = (SplitPane) getRoot();
		splitPane.getStyleClass().add("background");

		final UndoService undoService = new UndoService();

		sectionListPane = new SectionListPane<S, T>(mailService, undoService, settings.showExcludeBox().get());
		sectionListPane.setFocusTraversable(false);
		sectionListPane.setSectionDisableWhenZero(false);
		sectionListPane.setLazyCount(true);
		sectionListPane.setOnSelectTag(e -> refreshAfterTagSelected());
		sectionListPane.setOnSelectSection(e -> refreshAfterSectionSelect());
		sectionListPane.setOnUpdateSection(e -> refreshAfterSectionUpdate());
		sectionListPane.setOnUpdateTag(e -> refreshAfterTagUpdate());
		splitPane.getItems().add(sectionListPane);

		threadListPane = new ThreadListPane<S, T, H, M, C>(mailService, undoService, settings);
		threadListPane.prefHeightProperty().bind(splitPane.heightProperty());
		threadListPane.setOnSelectThread(e -> refreshAfterThreadSelected());
		threadListPane.setOnLoadThreadList(e -> refreshAfterThreadListLoad());
		threadListPane.setOnUpdatePattern(e -> refreshAfterPatternUpdate());
		threadListPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());
		splitPane.getItems().add(threadListPane);

		threadPane = new ThreadPane<S, T, H, M, C>(mailService, undoService, settings);
		threadPane.setFocusTraversable(false);
		threadPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());
		threadPane.managedProperty().bind(threadPane.visibleProperty());
		splitPane.getItems().add(threadPane);

		splitPane.setDividerPositions(.2, .5);
		splitPane.setOnKeyPressed(e -> toggleMiniMode(e));
		SplitPane.setResizableWithParent(sectionListPane, false);
		SplitPane.setResizableWithParent(threadListPane, false);
		SplitPane.setResizableWithParent(threadPane, true);
	}

	private void toggleMiniMode(final KeyEvent e) {
		final KeyCode code = e.getCode();
		final boolean left = code == KeyCode.LEFT;
		final boolean right = code == KeyCode.RIGHT;
		if (!left && !right) {
			return;
		}
		e.consume();

		final boolean toMini = splitPane.getItems().contains(threadPane);
		if (left && !toMini || right && toMini) {
			return;
		}

		final Window window = getWindow();
		final double newWidth = getWindow().getWidth() + (toMini? -1: 1) * (threadPane.getWidth() + 1);
		LOGGER.debug("cur -- {} = {} + {} + {}"
				, window.getWidth()
				, sectionListPane.getWidth()
				, threadListPane.getWidth()
				, threadPane.getWidth());
		LOGGER.debug("new -- {} {} {} = {}"
				, window.getWidth()
				, toMini? "-": "+"
				, threadPane.getWidth()
				, newWidth);

		Platform.runLater(() -> {
			if (toMini) {
				splitPane.getItems().remove(threadPane);
			} else {
				splitPane.getItems().add(threadPane);
				refreshAfterThreadSelected();
			}
			SplitPane.setResizableWithParent(threadListPane, toMini);
			SplitPane.setResizableWithParent(threadPane, !toMini);
			window.setWidth(newWidth);
		});

		LOGGER.debug("aft -- {} = {} + {} + {}"
				, window.getWidth()
				, sectionListPane.getWidth()
				, threadListPane.getWidth()
				, threadPane.getWidth());
	}

	public void setOnSignout(final EventHandler<ActionEvent> handler) {
		threadPane.setOnSignout(handler);

	}

	public void initData() {
//		sectionListPane.init("Bank", "HK HSBC");
		sectionListPane.init(GmailSection.SYSTEM.getName(), mailService.getSpecialTag(SpecialTag.INBOX).getName());

		refreshUnreadCount();

		mailService.addOnUpdateMessage(() -> Platform.runLater(() -> refreshAfterUpdateMessage()));
		mailService.addOnUpdateTagOrSection(() -> Platform.runLater(() -> refreshAfterUpdateTagOrSection()));

	}

	private void refreshUnreadCount() {
		final Set<T> includes = Collections.singleton(mailService.getSpecialTag(SpecialTag.UNREAD));
		ThreadPool.getDefault().submit(ThreadPriority.MIN, "counting unread messages", () -> {
			int count = 0;
			try {
				count = mailService.findThreads(includes, Collections.emptySet(), "", 200).size();
			} catch (final MailException e) {
				LOGGER.error("counting unread messages", e);
			}
			notificationService.setIconBadge("" + (count > 0? count: ""));
		});
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

	private void refreshAfterUpdateMessage() {
		if (!refreshAfterUpdateMessage) {
			return;
		}
		LOGGER.debug("refreshAfterUpdateMessage");
		LOGGER.info("message update detected");

		refreshUnreadCount();
		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	private void refreshAfterUpdateTagOrSection() {
		if (!refreshAfterUpdateTagOrSection) {
			return;
		}
		LOGGER.debug("refreshAfterUpdateTagOrSection");
		LOGGER.info("label update detected");

		refreshAfterSectionUpdate();
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

	private void refreshAfterThreadSelected() {
		if (!refreshAfterThreadSelected) {
			return;
		}
		LOGGER.debug("refreshAfterThreadSelected");

		final boolean isMini = !splitPane.getItems().contains(threadPane);
		if (isMini) {
			return;
		}

		final Set<H> threads = threadListPane.getSelectedThreads();
		if (threads.size() == 1 && threads.iterator().next() instanceof GmailMoreThreads) {
			refreshAfterMoreThreadsSelected();
			return;
		}

		// update thread details when (a) thread(s) is/are selected
		final boolean markRead = !sectionListPane.getIncludedOrSelectedTags().contains(mailService.getSpecialTag(SpecialTag.UNREAD));
		threadPane.refresh(threadListPane.getSelectedThreads(), markRead);
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

	private void refreshAfterThreadListLoad() {
		if (!refreshAfterThreadListLoad) {
			return;
		}
		LOGGER.debug("refreshAfterThreadListLoad");

		final boolean markRead = !sectionListPane.getIncludedOrSelectedTags().contains(mailService.getSpecialTag(SpecialTag.UNREAD));
		threadPane.refresh(threadListPane.getSelectedThreads(), markRead);
//		final String pattern = threadListPane.getNamePattern();
//		if (pattern.isEmpty()) {
			sectionListPane.updateItemCount(threadListPane.getThreadsTags(), threadListPane.getNamePattern(), true);
//		} else {
//			sectionListPane.refreshWithPattern(pattern);
//		}
	}

	private void refreshAfterTagSelected() {
		if (!refreshAfterTagSelected) {
			return;
		}
		LOGGER.debug("refreshAfterTagSelected");

		threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
	}

	private void refreshAfterPatternUpdate() {
		if (!refreshAfterPatternUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterPatternUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}

	private void refreshAfterThreadUpdate() {
		if (!refreshAfterThreadUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterThreadUpdate");

		sectionListPane.refreshAsync(() ->
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags()));
	}
}
