package net.anfoya.mail.browser.javafx.entrypoint;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.application.Application;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.security.auth.login.LoginException;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.javafx.scene.section.SectionListPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailBrowserApp extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowserApp.class);

	public static void main(final String[] args) {
		launch(args);
	}

	private SectionListPane<GmailSection, GmailTag> sectionListPane;
	private MailService<GmailSection, GmailTag, GmailThread, SimpleMessage> mailService;
	private ThreadListPane<GmailSection, GmailTag, GmailThread> threadListPane;
	private ThreadPane<GmailTag, GmailThread, SimpleMessage> threadPane;

	private ScheduledService<Long> refreshTimer;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.setOnCloseRequest(event -> {
			refreshTimer.cancel();
			ThreadPool.getInstance().shutdown();
		});

		mailService = new GmailService();
		ThreadPool.getInstance().submit(() -> {
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
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(5));

		final Scene scene = new Scene(mainPane, 1524, 780);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());

		final HBox selectionPane = new HBox();
		mainPane.setLeft(selectionPane);

		/* tag list */ {
			sectionListPane = new SectionListPane<GmailSection, GmailTag>(mailService, DND_THREADS_DATA_FORMAT);
			sectionListPane.setPrefWidth(250);
			sectionListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			sectionListPane.setSectionDisableWhenZero(false);
			sectionListPane.setTagChangeListener((ov, oldVal, newVal) -> {
				refreshThreadList();
			});
			sectionListPane.setUpdateSectionCallback(v -> {
				refreshThreadList();
				return null;
			});
			selectionPane.getChildren().add(sectionListPane);
		}

		/* thread list */ {
			threadListPane = new ThreadListPane<GmailSection, GmailTag, GmailThread>(mailService);
			threadListPane.setPrefWidth(250);
			threadListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			threadListPane.addSelectionListener((ov, oldVal, newVal) -> {
				if (!threadListPane.isRefreshing()) {
					// update thread details when (a) thread(s) is/are selected
					refreshThread();
				}
			});
			threadListPane.addChangeListener(change -> {
				// update thread count when a new thread list is loaded
				updateThreadCount();
				if (!threadListPane.isRefreshing()) {
					// update thread details in case no thread is selected
					refreshThread();
				}
			});

			selectionPane.getChildren().add(threadListPane);
		}

		/* movie panel */ {
			threadPane = new ThreadPane<GmailTag, GmailThread, SimpleMessage>(mailService);
			threadPane.setOnDelTag(event -> {
				refreshSectionList();
				refreshThreadList();
			});
			/*
			moviePane.setOnCreateTag(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					refreshSectionList();
					refreshMovieList();
				}
			});
			moviePane.setOnUpdateMovie(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					refreshMovieList();
				}
			});
			*/
			mainPane.setCenter(threadPane);
		}

		primaryStage.setTitle("FisherMail / Agaar / Agamar / Agaram");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Mail.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void initData() {
		sectionListPane.refresh();
//		sectionListPane.selectTag(GmailSection.GMAIL_SYSTEM, "INBOX");
		sectionListPane.selectTag("Bank", "HK HSBC");
		sectionListPane.expand(GmailSection.GMAIL_SYSTEM);

		refreshTimer = new ScheduledService<Long>() {
			@Override
			protected Task<Long> createTask() {
				return new Task<Long>() {
					@Override
					protected Long call() throws Exception {
						LOGGER.info("refresh started...");
						return System.currentTimeMillis();
					}
				};
			}
		};
		refreshTimer.setOnSucceeded(event -> {
			refreshThreadList();
			LOGGER.info("refresh finished! ({}ms)", System.currentTimeMillis() - (Long) event.getSource().getValue());
		});
		refreshTimer.setDelay(Duration.seconds(20));
		refreshTimer.setPeriod(Duration.seconds(20));
		refreshTimer.setExecutor(ThreadPool.getInstance().getExecutor());
		refreshTimer.start();

	}

	private void refreshSectionList() {
		sectionListPane.refreshAsync();
		threadPane.refreshTags();
	}

	private void refreshThread() {
		final Set<GmailThread> selectedThreads = threadListPane.getSelectedThreads();
		threadPane.refresh(selectedThreads);
	}

	private void updateThreadCount() {
		final int currentCount = threadListPane.getThreadCount();
		final Set<GmailTag> availableTags = threadListPane.getThreadTags();
		final String namePattern = "";
		sectionListPane.updateCount(currentCount, availableTags, namePattern);
	}

	private void refreshThreadList() {
		threadListPane.refreshWithTags(sectionListPane.getAllTags(), sectionListPane.getIncludedTags(), sectionListPane.getExcludedTags());
	}
}
