package net.anfoya.mail.browser.javafx;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.paint.Color;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Notification.Notifier;
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

	private final SectionListPane<S, T> sectionListPane;
	private final ThreadListPane<S, T, H, M, C> threadListPane;
	private final ThreadPane<S, T, H, M, C> threadPane;

	public MailBrowser(final MailService<S, T, H, M, C> mailService) throws MailException {
		super(new SplitPane(), Color.TRANSPARENT);
		this.mailService = mailService;
		final UndoService undoService = new UndoService();

		StyleHelper.addCommonCss(this);
		StyleHelper.addCss(this, "/net/anfoya/javafx/scene/control/excludebox.css");

		setOnKeyPressed(e -> {
			switch(e.getCode()) {
			case RIGHT:
			case LEFT:
				LOGGER.warn("{}", e.getCode());
				e.consume();
				break;
			default:
			}
		});

		final boolean showExcBox = Settings.getSettings().showExcludeBox().get();
		final SplitPane splitPane = (SplitPane) getRoot();
		splitPane.getStyleClass().add("background");
		if (showExcBox) {
			splitPane.setDividerPosition(0, .10);
			splitPane.setDividerPosition(1, .34);
		} else {
			splitPane.setDividerPosition(0, .08);
			splitPane.setDividerPosition(1, .32);
		}

		sectionListPane = new SectionListPane<S, T>(mailService, undoService, showExcBox);
		sectionListPane.setFocusTraversable(false);
		sectionListPane.prefHeightProperty().bind(sectionListPane.heightProperty());
		sectionListPane.setSectionDisableWhenZero(false);
		sectionListPane.setLazyCount(true);
		sectionListPane.setOnSelectTag(e -> refreshAfterTagSelected());
		sectionListPane.setOnSelectSection(e -> refreshAfterSectionSelect());
		sectionListPane.setOnUpdateSection(e -> refreshAfterSectionUpdate());
		sectionListPane.setOnUpdateTag(e -> refreshAfterTagUpdate());
		splitPane.getItems().add(sectionListPane);

		threadListPane = new ThreadListPane<S, T, H, M, C>(mailService, undoService);
		threadListPane.prefHeightProperty().bind(splitPane.heightProperty());
		threadListPane.setOnSelectThread(e -> refreshAfterThreadSelected());
		threadListPane.setOnLoadThreadList(e -> refreshAfterThreadListLoad());
		threadListPane.setOnUpdatePattern(e -> refreshAfterPatternUpdate());
		threadListPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());
		splitPane.getItems().add(threadListPane);

		threadPane = new ThreadPane<S, T, H, M, C>(mailService, undoService);
		threadPane.setFocusTraversable(false);
		threadPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());
		splitPane.getItems().add(threadPane);

		Notifier.INSTANCE.popupLifetime().bind(Settings.getSettings().popupLifetime());
	}

	public void setOnSignout(final EventHandler<ActionEvent> handler) {
		threadPane.setOnSignout(handler);

	}

	public void initData() {
//		sectionListPane.init("Bank", "HK HSBC");
		try {
			sectionListPane.init(GmailSection.SYSTEM.getName(), mailService.getSpecialTag(SpecialTag.INBOX).getName());
		} catch (final MailException e) {
			LOGGER.error("going to System / Inbox", e);
		}

		mailService.addOnUpdateMessage(p -> {
			Platform.runLater(() -> refreshAfterUpdateMessage());
			return null;
		});
		mailService.addOnUpdateTagOrSection(p -> {
			Platform.runLater(() -> refreshAfterUpdateTagOrSection());
			return null;
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

		sectionListPane.refreshAsync(v -> {
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
			return null;
		});

		if (System.getProperty("os.name").contains("OS X")) {
			ThreadPool.getInstance().submitLow(() -> {
				final Set<T> includes = new HashSet<T>();
				int unreadCount = 0;
				try {
					includes.add(mailService.getSpecialTag(SpecialTag.UNREAD));
					unreadCount = mailService.findThreads(includes, Collections.emptySet(), "", 200).size();
				} catch (final MailException e) {
					LOGGER.error("counting unread messages", e);
				}
				if (unreadCount > 0) {
					com.apple.eawt.Application.getApplication().setDockIconBadge("" + unreadCount);
				} else {
					com.apple.eawt.Application.getApplication().setDockIconBadge(null);
				}
			}, "counting unread messages");
		}
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

		sectionListPane.refreshAsync(v -> {
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
			return null;
		});
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

		sectionListPane.refreshAsync(v -> {
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
			return null;
		});
	}

	private void refreshAfterThreadSelected() {
		if (!refreshAfterThreadSelected) {
			return;
		}
		LOGGER.debug("refreshAfterThreadSelected");

		final Set<H> threads = threadListPane.getSelectedThreads();
		if (threads.size() == 1 && threads.iterator().next() instanceof GmailMoreThreads) {
			refreshAfterMoreThreadsSelected();
			return;
		}

		// update thread details when (a) thread(s) is/are selected
		threadPane.refresh(threadListPane.getSelectedThreads());
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

		threadPane.refresh(threadListPane.getSelectedThreads());
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

		sectionListPane.refreshAsync(e -> {
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
			return null;
		});
	}

	private void refreshAfterThreadUpdate() {
		if (!refreshAfterThreadUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterThreadUpdate");

		sectionListPane.refreshAsync(e -> {
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
			return null;
		});
	}
}
