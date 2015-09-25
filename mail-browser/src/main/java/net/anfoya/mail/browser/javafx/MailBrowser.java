package net.anfoya.mail.browser.javafx;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Notification;
import net.anfoya.javafx.scene.control.Notification.Notifier;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailBrowser<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowser.class);

	private static final Duration NOTIFIER_LIFETIME = Duration.seconds(20);
	private static final boolean SHOW_EXCLUDE_BOX = Settings.getSettings().showExcludeBox().get();

	private final MailService<S, T, H, M, C> mailService;

	private SectionListPane<S, T> sectionListPane;
	private ThreadListPane<S, T, H, M, C> threadListPane;
	private ThreadPane<T, H, M, C> threadPane;

	private boolean quit;

	public MailBrowser(final MailService<S, T, H, M, C> mailService) throws MailException {
		this.mailService = mailService;

		quit = true;

		initGui();

		setOnShowing(e -> initData());
		setOnCloseRequest(e -> Notifier.INSTANCE.stop());
	}

	@Override
	public void hide() {
		//TODO
		super.hide();
//		if (OperatingSystem.getInstance().getFamily().equals(Family.MAC)) {
//			setIconified(true);
//		} else {
//			super.hide();
//		}
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

		final Scene scene = new Scene(splitPane);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/button_flat.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/mail/css/Mail.css").toExternalForm());

		/* section+tag list */ {
			sectionListPane = new SectionListPane<S, T>(mailService, DND_THREADS_DATA_FORMAT, SHOW_EXCLUDE_BOX);
			sectionListPane.prefHeightProperty().bind(sectionListPane.heightProperty());
			sectionListPane.setSectionDisableWhenZero(false);
			sectionListPane.setLazyCount(true);
			sectionListPane.setOnSelectTag(e -> refreshAfterTagSelected());
			sectionListPane.setOnUpdateSection(e -> refreshAfterSectionUpdate());
			sectionListPane.setOnUpdateTag(e -> refreshAfterTagUpdate());
			splitPane.getItems().add(sectionListPane);
		}

		/* thread list */ {
			threadListPane = new ThreadListPane<S, T, H, M, C>(mailService);
			threadListPane.prefHeightProperty().bind(splitPane.heightProperty());
			threadListPane.setOnSelectThread(e -> refreshAfterThreadSelected());
			threadListPane.setOnLoadThreadList(e -> refreshAfterThreadListLoad());
			threadListPane.setOnUpdatePattern(e -> refreshAfterPatternUpdate());
			threadListPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());
			splitPane.getItems().add(threadListPane);
		}

		/* thread panel */ {
			threadPane = new ThreadPane<T, H, M, C>(mailService);
			threadPane.setOnUpdateThread(e -> refreshAfterThreadUpdate());
			threadPane.setOnLogout(e -> logout());
			splitPane.getItems().add(threadPane);
		}

        setScene(scene);

		Notifier.INSTANCE.setPopupLifetime(NOTIFIER_LIFETIME);

        Platform.runLater(() -> {
        	threadListPane.requestFocus();
    		toFront();
        });
	}

	private void logout() {
		quit = false;
		mailService.disconnect();
		close();
	}

	private void initData() {
//		sectionListPane.init("Bank", "HK HSBC");
		sectionListPane.init(GmailSection.SYSTEM.getName(), "Inbox");

		mailService.addOnUpdateMessage(v -> {
			Platform.runLater(() -> refreshAfterRemoteUpdate());
			return null;
		});

		mailService.addOnUnreadMessage(tList -> {
			final int count = tList.size();
			final String message = count + " new message" + (count == 0? "": "s");
			Platform.runLater(() -> Notifier.INSTANCE.notify("FisherMail", message, Notification.SUCCESS_ICON));
			return null;
		});

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

	boolean refreshAfterTagSelected = true;
	boolean refreshAfterThreadSelected = true;
	boolean refreshAfterMoreResultsSelected = true;

	boolean refreshAfterThreadListLoad = true;

	boolean refreshAfterTagUpdate = true;
	boolean refreshAfterSectionUpdate = true;
	boolean refreshAfterThreadUpdate = true;
	boolean refreshAfterPatternUpdate = true;
	boolean refreshAfterRemoteUpdate = true;

	private void refreshAfterRemoteUpdate() {
		if (!refreshAfterRemoteUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterRemoteUpdate");
		LOGGER.info("update detected");

		sectionListPane.refreshAsync(v -> {
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
			return null;
		});
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
		sectionListPane.updateItemCount(threadListPane.getThreadsTags(), threadListPane.getNamePattern(), true);
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
