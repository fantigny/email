package net.anfoya.mail.browser.javafx.entrypoint;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;
import javafx.application.Application;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.security.auth.login.LoginException;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.javafx.scene.section.SectionListPane;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailBrowserApp<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread, M extends SimpleMessage> extends Application{
	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowserApp.class);

	public static void main(final String[] args) {
		launch(args);
	}

	private SectionListPane<S, T> sectionListPane;
	private MailService<S, T, H, M> mailService;
	private ThreadListPane<S, T, H, M> threadListPane;
	private ThreadPane<T, H, M> threadPane;

	private ScheduledService<Boolean> refreshService;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.initStyle(StageStyle.UNIFIED);
		primaryStage.setOnCloseRequest(event -> {
			refreshService.cancel();
			ThreadPool.getInstance().shutdown();
		});

		@SuppressWarnings("unchecked")
		final MailService<S, T, H, M> mailService = (MailService<S, T, H, M>) new GmailService();
		this.mailService = mailService;
		ThreadPool.getInstance().submitHigh(() -> {
			try {
				mailService.login(null, null);
			} catch (final MailException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) throws MailException, LoginException {
		final SplitPane splitPane = new SplitPane();
		splitPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent");

		final Scene scene = new Scene(splitPane, 1400, 700);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());

		/* section+tag list */ {
			sectionListPane = new SectionListPane<S, T>(mailService, DND_THREADS_DATA_FORMAT);
			sectionListPane.setPadding(new Insets(5, 0, 5, 5));
			sectionListPane.setMinWidth(150);
			sectionListPane.prefHeightProperty().bind(sectionListPane.heightProperty());
			sectionListPane.setSectionDisableWhenZero(false);
			sectionListPane.setLazyCount(true);
			sectionListPane.setOnSelectTag(event -> refreshAfterTagSelected());
			sectionListPane.setOnUpdateSection(event -> refreshAfterSectionUpdate());
			splitPane.getItems().add(sectionListPane);
		}

		/* thread list */ {
			threadListPane = new ThreadListPane<S, T, H, M>(mailService);
			threadListPane.setMinWidth(250);
			threadListPane.prefHeightProperty().bind(splitPane.heightProperty());
			threadListPane.setOnSelectThread(event -> refreshAfterThreadSelected());
			threadListPane.setOnLoadThreadList(event -> refreshAfterThreadListLoad());
			threadListPane.setOnUpdatePattern(event -> refreshAfterPatternUpdate());
			threadListPane.setOnUpdateThread(event -> refreshAfterThreadUpdate());
			splitPane.getItems().add(threadListPane);
		}

		/* tool bar */ {
			final Button refreshButton = new Button("r");
			refreshButton.setOnAction(event -> refreshAfterRemoteUpdate());
			final ToolBar toolBar = new ToolBar(refreshButton);
			toolBar.setOrientation(Orientation.VERTICAL);
//			centerPane.getChildren().add(toolBar);
		}

		/* thread panel */ {
			threadPane = new ThreadPane<T, H, M>(mailService);
			threadPane.setPadding(new Insets(5, 3, 5, 0));
			threadPane.setOnUpdateThread(event -> refreshAfterThreadUpdate());
			splitPane.getItems().add(threadPane);
		}

		splitPane.setDividerPosition(0, .15);
		splitPane.setDividerPosition(1, .35);

		primaryStage.setTitle("FisherMail / Agaar / Agamar / Agaram");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Mail.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void initData() {
		sectionListPane.refreshAsync(v -> {
			sectionListPane.selectTag(GmailSection.SYSTEM.getName(), "Inbox");
//			sectionListPane.selectTag("Bank", "HK HSBC");
			return null;
		});

		refreshService = new ScheduledService<Boolean>() {
			@Override
			protected Task<Boolean> createTask() {
				return new Task<Boolean>() {
					@Override
					protected Boolean call() throws Exception {
						return mailService.hasUpdate();
					}
				};
			}
		};
		refreshService.setOnFailed(event -> {
			// TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out);
		});
		refreshService.setOnSucceeded(event -> {
			if (refreshService.getValue()) {
				LOGGER.info("update detected");
				refreshAfterRemoteUpdate();
			}
		});
		refreshService.setDelay(Duration.seconds(60));
		refreshService.setPeriod(Duration.seconds(60));
		refreshService.setExecutor(ThreadPool.getInstance().getExecutor());
		refreshService.start();
	}

	boolean refreshAfterTagSelected = true;
	boolean refreshAfterThreadSelected = true;

	boolean refreshAfterSectionUpdate = true;
	boolean refreshAfterThreadUpdate = true;
	boolean refreshAfterThreadListLoad = true;

	boolean refreshAfterRemoteUpdate = true;

	boolean refreshAfterPatternUpdate = false;

	private void refreshAfterRemoteUpdate() {
		if (!refreshAfterRemoteUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterRemoteUpdate");

		sectionListPane.refreshAsync();
	}

	private void refreshAfterSectionUpdate() {
		if (!refreshAfterSectionUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterSectionUpdate");

		sectionListPane.refreshAsync();
	}

	private void refreshAfterThreadSelected() {
		if (!refreshAfterThreadSelected) {
			return;
		}
		LOGGER.debug("refreshAfterThreadSelected");

		// update thread details when (a) thread(s) is/are selected
		threadPane.refresh(threadListPane.getSelectedThreads());
	}

	private void refreshAfterThreadListLoad() {
		if (!refreshAfterThreadListLoad) {
			return;
		}
		LOGGER.debug("refreshAfterThreadListLoad");

		// refresh tags when a new list of threads is loaded
		sectionListPane.refreshWithTags(
				threadListPane.getThreadsTags()
				, threadListPane.getThreadCount()
				, true);

		// update thread details in case no thread is selected
		threadPane.refresh(threadListPane.getSelectedThreads());
	}

	private void refreshAfterTagSelected() {
		if (!refreshAfterTagSelected) {
			return;
		}
		LOGGER.debug("refreshAfterTagSelected");

		threadListPane.refreshWithTags(sectionListPane.getIncludedTagsNew(), sectionListPane.getExcludedTagsNew());
	}

	private void refreshAfterPatternUpdate() {
		if (!refreshAfterPatternUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterPatternUpdate");

		sectionListPane.refreshWithPattern(threadListPane.getNamePattern());
	}

	private void refreshAfterThreadUpdate() {
		if (!refreshAfterThreadUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterThreadUpdate");

		sectionListPane.refreshAsync();
		threadListPane.refreshWithTags(sectionListPane.getIncludedTagsNew(), sectionListPane.getExcludedTagsNew());
	}
}
