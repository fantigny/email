package net.anfoya.mail.browser.javafx.entrypoint;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.application.Application;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.security.auth.login.LoginException;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.NewMail;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailMessage;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
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
	private MailService<GmailSection, GmailTag, GmailThread, GmailMessage> mailService;
	private ThreadListPane<GmailSection, GmailTag, GmailThread> threadListPane;
	private ThreadPane<GmailTag, GmailThread, GmailMessage> threadPane;

	private ScheduledService<Boolean> refreshTimer;

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

		final Scene scene = new Scene(mainPane, 1400, 700);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());

		final HBox selectionPane = new HBox();
		mainPane.setLeft(selectionPane);

		/* section+tag list */ {
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

		final HBox centerPane = new HBox(0);
		mainPane.setCenter(centerPane);

		/* tool bar */ {
			final Button newButton = new Button("n");
			newButton.setOnAction(event -> {
				new NewMail();
			});
			final Button refreshButton = new Button("r");
			refreshButton.setOnAction(event -> {
				refreshSectionList();
				refreshThreadList();
			});
			final ToolBar toolBar = new ToolBar(newButton, refreshButton);
			toolBar.setOrientation(Orientation.VERTICAL);
//			centerPane.getChildren().add(toolBar);
		}

		/* thread panel */ {
			threadPane = new ThreadPane<GmailTag, GmailThread, GmailMessage>(mailService);
			threadPane.prefWidthProperty().bind(centerPane.widthProperty());
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
			centerPane.getChildren().add(threadPane);
		}

		primaryStage.setTitle("FisherMail / Agaar / Agamar / Agaram");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Mail.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void initData() {
		sectionListPane.refresh();

		sectionListPane.selectTag(GmailSection.SYSTEM.getName(), "INBOX");
		sectionListPane.expand(GmailSection.SYSTEM.getName());

//		sectionListPane.selectTag("Bank", "HK HSBC");
//		sectionListPane.expand("Bank");

		refreshTimer = new ScheduledService<Boolean>() {
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
		refreshTimer.setOnSucceeded(event -> {
			if ((boolean) event.getSource().getValue()) {
				LOGGER.info("update detected");
				refreshSectionList();
				refreshThreadList();
			}
		});
		refreshTimer.setDelay(Duration.seconds(3));
		refreshTimer.setPeriod(Duration.seconds(3));
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
