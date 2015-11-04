package net.anfoya.mail.browser.javafx;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Notification.Notifier;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.SpecialTag;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.javafx.scene.section.SectionListPane;

public class MailBrowser<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowser.class);

	private static final boolean SHOW_EXCLUDE_BOX = Settings.getSettings().showExcludeBox().get();

	private final MailService<S, T, H, M, C> mailService;

	private SectionListPane<S, T> sectionListPane;
	private ThreadListPane<S, T, H, M, C> threadListPane;
	private ThreadPane<T, H, M, C> threadPane;

	private boolean quit;

	public MailBrowser(final MailService<S, T, H, M, C> mailService) throws MailException {
		this.mailService = mailService;
		this.quit = true;

		setOnShowing(e -> initData());
		setOnCloseRequest(e -> Notifier.INSTANCE.stop());

		initMacOs();
		initGui();
	}

	private void initGui() throws MailException {
		initStyle(StageStyle.UNIFIED);
		setWidth(1400);
		setHeight(800);
		setTitle("FisherMail");
		getIcons().add(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png")));

		final SplitPane splitPane = new SplitPane();
		splitPane.getStyleClass().add("background");
		if (SHOW_EXCLUDE_BOX) {
			splitPane.setDividerPosition(0, .10);
			splitPane.setDividerPosition(1, .34);
		} else {
			splitPane.setDividerPosition(0, .08);
			splitPane.setDividerPosition(1, .32);
		}

		sectionListPane = new SectionListPane<S, T>(mailService, DND_THREADS_DATA_FORMAT, SHOW_EXCLUDE_BOX);
		sectionListPane.setFocusTraversable(false);
		sectionListPane.prefHeightProperty().bind(sectionListPane.heightProperty());
		sectionListPane.setSectionDisableWhenZero(false);
		sectionListPane.setLazyCount(true);
		sectionListPane.setOnSelectTag(e -> refreshAfterTagSelected());
		sectionListPane.setOnSelectSection(e -> refreshAfterSectionSelect());
		sectionListPane.setOnUpdateSection(e -> refreshAfterSectionUpdate());
		sectionListPane.setOnUpdateTag(e -> refreshAfterTagUpdate());
		splitPane.getItems().add(sectionListPane);

		threadListPane = new ThreadListPane<S, T, H, M, C>(mailService);
		threadListPane.prefHeightProperty().bind(splitPane.heightProperty());
		threadListPane.setOnSelectThread(e -> refreshAfterThreadSelected());
		threadListPane.setOnLoadThreadList(e -> refreshAfterThreadListLoad());
		threadListPane.setOnUpdatePattern(e -> refreshAfterPatternUpdate());
		threadListPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());
		splitPane.getItems().add(threadListPane);

		threadPane = new ThreadPane<T, H, M, C>(mailService);
		threadPane.setFocusTraversable(false);
		threadPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());
		threadPane.setOnLogout(e -> signout());
		splitPane.getItems().add(threadPane);

		final Scene scene = new Scene(splitPane);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/button_flat.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/mail/css/Mail.css").toExternalForm());
        setScene(scene);

		Notifier.INSTANCE.popupLifetime().bind(Settings.getSettings().popupLifetime());
		Notifier.INSTANCE.setCallback(v -> {
			if (isIconified()) {
				setIconified(false);
			}
			if (!isFocused()) {
				requestFocus();
			}
			return null;
		});
	}

	private void initMacOs() {
		if (!System.getProperty("os.name").contains("OS X")) {
			return;
		}
		LOGGER.info("initialize OS X stage behaviour");
//		Platform.setImplicitExit(false);
//		com.apple.eawt.Application.getApplication().addAppEventListener(new AppReOpenedListener() {
//			@Override
//			public void appReOpened(final AppReOpenedEvent e) {
//				LOGGER.info("OS X AppReOpenedListener");
//				if (!isShowing()) {
//					LOGGER.debug("OS X show()");
//					Platform.runLater(() -> show());
//				}
//				if (isIconified()) {
//					LOGGER.debug("OS X setIconified(false)");
//					Platform.runLater(() -> setIconified(false));
//				}
//				if (!isFocused()) {
//					LOGGER.debug("OS X requestFocus()");
//					Platform.runLater(() -> requestFocus());
//				}
//			}
//		});

//		final List<MenuBase> menus = new ArrayList<>();
//		menus.add(GlobalMenuAdapter.adapt(new Menu("Test")));
//
//		Toolkit.getToolkit().getSystemMenu().setMenus(menus);
	}

	private void signout() {
		final Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to sign out?");
		alert.setTitle("signing out");
		alert.setHeaderText(null);
		alert.showAndWait()
			.filter(response -> response == ButtonType.OK)
			.ifPresent(response -> {
				quit = false;
				mailService.disconnect();
				close();
			});
	}

	private void initData() {
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

		mailService.addOnNewMessage(p -> notifyAfterNewMessage(p));

		final Task<String> titleTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				final C contact = mailService.getContact();
				if (contact.getFullname().isEmpty()) {
					return contact.getEmail();
				} else {
					return contact.getFullname() + " (" + contact.getEmail() + ")";
				}
			}
		};
		titleTask.setOnSucceeded(e -> setTitle(getTitle() + " - " + e.getSource().getValue()));
		titleTask.setOnFailed(e -> LOGGER.error("loading user's name", e.getSource().getException()));
		ThreadPool.getInstance().submitLow(titleTask, "loading user's name");
	}

	private final boolean notifyAfterNewMessage = true;

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

	private Void notifyAfterNewMessage(final Set<H> threads) {
		if (!notifyAfterNewMessage) {
			return null;
		}
		LOGGER.debug("notifyAfterNewMessage");

		threads.forEach(t -> {
			ThreadPool.getInstance().submitLow(() -> {
				final String message;
				try {
					final M m = mailService.getMessage(t.getLastMessageId());
					message = "from " + String.join(", ", MessageHelper.getNames(m.getMimeMessage().getFrom()))
							+ "\r\n" + m.getSnippet();
				} catch (final Exception e) {
					LOGGER.error("notifying new message for thread {}", t.getId(), e);
					return;
				}
				Platform.runLater(() -> Notifier.INSTANCE.notifySuccess(
						t.getSubject()
						, message));
			}, "notifying new message");
		});

		return null;
	}

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

	public boolean isQuit() {
		return quit;
	}
}
