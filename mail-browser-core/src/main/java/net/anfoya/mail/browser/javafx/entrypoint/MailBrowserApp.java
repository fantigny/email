package net.anfoya.mail.browser.javafx.entrypoint;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import javax.security.auth.login.LoginException;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.gmail.GMailException;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleSection;
import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.javafx.scene.section.SectionListPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailBrowserApp<
		S extends SimpleSection
		, T extends SimpleTag
		, H extends SimpleThread
		, M extends SimpleMessage
		, C extends SimpleContact>
		extends Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowserApp.class);

	public static void main(final String[] args) {
		launch(args);
	}

	private SectionListPane<S, T> sectionListPane;
	private MailService<S, T, H, M, C> mailService;
	private ThreadListPane<S, T, H, M, C> threadListPane;
	private ThreadPane<T, H, M> threadPane;

	@Override
	public void init() {
		try {
			@SuppressWarnings("unchecked")
			final MailService<S, T, H, M, C> mailService = (MailService<S, T, H, M, C>) new GmailService().login("main");
			this.mailService = mailService;
		} catch (final GMailException e) {
			LOGGER.error("login error", e);
			System.exit(1);
		}
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.initStyle(StageStyle.UNIFIED);
		primaryStage.setOnCloseRequest(event -> {
			ThreadPool.getInstance().shutdown();
		});

		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) throws MailException, LoginException {
		final SplitPane splitPane = new SplitPane();
		splitPane.getStyleClass().add("background");

		final Scene scene = new Scene(splitPane, 1400, 800);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("MailBrowserApp.css").toExternalForm());

		/* section+tag list */ {
			sectionListPane = new SectionListPane<S, T>(mailService, DND_THREADS_DATA_FORMAT);
			sectionListPane.setPadding(new Insets(5, 0, 5, 5));
			sectionListPane.setMinWidth(150);
			sectionListPane.prefHeightProperty().bind(sectionListPane.heightProperty());
			sectionListPane.setSectionDisableWhenZero(false);
			sectionListPane.setLazyCount(true);
			sectionListPane.setOnSelectTag(event -> refreshAfterTagSelected());
			sectionListPane.setOnUpdateSection(event -> refreshAfterSectionUpdate());
			sectionListPane.setOnUpdateTag(event -> refreshAfterTagUpdate());
			splitPane.getItems().add(sectionListPane);
		}

		/* thread list */ {
			threadListPane = new ThreadListPane<S, T, H, M, C>(mailService);
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

		splitPane.setDividerPosition(0, .14);
		splitPane.setDividerPosition(1, .38);

		primaryStage.setTitle("FisherMail / Agaar / Agamar / Agaram");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Mail.png")));
        primaryStage.setScene(scene);
        primaryStage.show();

		sectionListPane.requestFocus();
	}

	private void initData() {

		sectionListPane.init(GmailSection.SYSTEM.getName(), "Inbox");

		mailService.addOnUpdate(new Callback<Throwable, Void>() {
			@Override
			public Void call(final Throwable t) {
				if (t != null) {
					// TODO Auto-generated catch block
					t.printStackTrace();
					return null;
				}
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						LOGGER.info("update detected");
						refreshAfterRemoteUpdate();
					}
				});
				return null;
			}
		});
	}

	boolean refreshAfterTagSelected = true;
	boolean refreshAfterThreadSelected = true;

	boolean refreshAfterTagUpdate = true;
	boolean refreshAfterSectionUpdate = true;
	boolean refreshAfterThreadUpdate = true;
	boolean refreshAfterThreadListLoad = true;

	boolean refreshAfterRemoteUpdate = true;

	boolean refreshAfterPatternUpdate = false;

	boolean refreshAfterMoreResultsSelected = true;

	private void refreshAfterRemoteUpdate() {
		if (!refreshAfterRemoteUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterRemoteUpdate");

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

		sectionListPane.refreshAsync(new Callback<Void, Void>() {
			@Override
			public Void call(final Void v) {
				threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
				return null;
			}
		});
	}

	private void refreshAfterTagUpdate() {
		if (!refreshAfterTagUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterTagUpdate");

		sectionListPane.refreshAsync(event -> {
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

		sectionListPane.updateItemCount(threadListPane.getThreadsTags(), threadListPane.getNamePattern(), false);
	}

	private void refreshAfterThreadUpdate() {
		if (!refreshAfterThreadUpdate) {
			return;
		}
		LOGGER.debug("refreshAfterThreadUpdate");

		sectionListPane.refreshAsync(event -> {
			threadListPane.refreshWithTags(sectionListPane.getIncludedOrSelectedTags(), sectionListPane.getExcludedTags());
			return null;
		});
	}
}
