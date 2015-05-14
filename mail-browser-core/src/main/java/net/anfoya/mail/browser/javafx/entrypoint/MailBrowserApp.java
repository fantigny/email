package net.anfoya.mail.browser.javafx.entrypoint;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.HashSet;
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
import net.anfoya.mail.browser.javafx.MailComposer;
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
		primaryStage.setOnCloseRequest(event -> {
			refreshService.cancel();
			ThreadPool.getInstance().shutdown();
		});

		mailService = (MailService<S, T, H, M>) new GmailService();
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
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(5));

		final Scene scene = new Scene(mainPane, 1400, 700);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());

		final HBox selectionPane = new HBox();
		mainPane.setLeft(selectionPane);

		/* section+tag list */ {
			sectionListPane = new SectionListPane<S, T>(mailService, DND_THREADS_DATA_FORMAT);
			sectionListPane.setPrefWidth(250);
			sectionListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			sectionListPane.setSectionDisableWhenZero(false);
			sectionListPane.setLazyCount(true);
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
			threadListPane = new ThreadListPane<S, T, H, M>(mailService);
			threadListPane.setPrefWidth(250);
			threadListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			threadListPane.addSelectionListener((ov, oldVal, newVal) -> {
				if (!threadListPane.isRefreshing()) {
					// update thread details when (a) thread(s) is/are selected
					refreshThread();
				}
			});
			threadListPane.addChangeListener(change -> {
				// refresh tags when a new list of threads is loaded
				sectionListPane.refreshWithTags(threadListPane.getThreadsTags(), threadListPane.getThreadCount());
				if (!threadListPane.isRefreshing()) {
					// update thread details in case no thread is selected
					refreshThread();
				}
			});
			threadListPane.addTagChangeListener((ov, oldVal, newVal) -> {
				sectionListPane.refreshWithNamePattern(newVal);
			});
			selectionPane.getChildren().add(threadListPane);
		}

		final HBox centerPane = new HBox(0);
		mainPane.setCenter(centerPane);

		/* tool bar */ {
			final Button newButton = new Button("n");
			newButton.setOnAction(event -> {
				try {
					new MailComposer<M>(mailService);
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
			threadPane = new ThreadPane<T, H, M>(mailService);
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
		sectionListPane.refreshAsync(param -> {
			sectionListPane.selectTag(GmailSection.SYSTEM.getName(), "Inbox");
			threadListPane.refreshWithTags(sectionListPane.getIncludedTags(), sectionListPane.getExcludedTags());
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
				refreshThreadList();
			}
		});
		refreshService.setDelay(Duration.seconds(60));
		refreshService.setPeriod(Duration.seconds(60));
		refreshService.setExecutor(ThreadPool.getInstance().getExecutor());
		refreshService.start();
	}

	private void refreshSectionList() {
		sectionListPane.refreshAsync();
	}

	private void refreshThread() {
		final Set<H> selectedThreads = threadListPane.getSelectedThreads();
		threadPane.refresh(selectedThreads);
	}

	private void refreshThreadList() {
		final Set<T> excluded = sectionListPane.getExcludedTags();
		Set<T> included = sectionListPane.getIncludedTags();
		if (included.isEmpty() && excluded.isEmpty()) {
			final T tag = sectionListPane.getSelectedTag();
			if (tag != null) {
				included = new HashSet<T>();
				included.add(tag);
			}
		}

		threadListPane.refreshWithTags(included, excluded);
	}
}
