package net.anfoya.mail.browser.javafx.entrypoint;

import static net.anfoya.tag.javafx.scene.control.SectionListPane.DND_TAG_DATA_FORMAT;

import java.util.Set;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.security.auth.login.LoginException;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.ThreadListPane;
import net.anfoya.mail.browser.javafx.ThreadPane;
import net.anfoya.mail.gmail.GmailImpl;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.javafx.scene.control.SectionListPane;
import net.anfoya.tag.service.TagServiceException;

public class MailBrowserApp extends Application {
//	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowserApp.class);
	private static final DataFormat DND_THREADS_DATA_FORMAT = new DataFormat("fishermail-thread");

	public static void main(final String[] args) {
		launch(args);
	}

	private SectionListPane<GmailSection, GmailTag> sectionListPane;
	private MailService<GmailSection, GmailTag, GmailThread> mailService;
	private ThreadListPane<GmailSection, GmailTag, GmailThread> threadListPane;
	private ThreadPane<GmailSection, GmailTag, GmailThread> threadPane;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(final WindowEvent event) {
				ThreadPool.getInstance().shutdown();
			}
		});

		mailService = new GmailImpl();
		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					mailService.login(null, null);
				} catch (final LoginException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) throws MailServiceException, LoginException {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(5));

		final Scene scene = new Scene(mainPane, 1524, 780);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());

		final HBox selectionPane = new HBox();
		mainPane.setLeft(selectionPane);

		/* tag list */ {
			sectionListPane = new SectionListPane<GmailSection, GmailTag>(mailService);
			sectionListPane.setPrefWidth(250);
			sectionListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			sectionListPane.setSectionDisableWhenZero(false);
			sectionListPane.setTagChangeListener((ov, oldVal, newVal) -> {
				refreshThreadList();
			});
			sectionListPane.setUpdateSectionCallback(v -> {
				updateThreadCount();
				return null;
			});
			sectionListPane.setOnTagDragOver(event -> {
				if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
					event.acceptTransferModes(TransferMode.ANY);
				}

				event.consume();
			});
			sectionListPane.setOnTagDragDropped(event -> {
				final Dragboard db = event.getDragboard();
				if (db.hasContent(DND_THREADS_DATA_FORMAT) && db.hasContent(DND_TAG_DATA_FORMAT)) {
					try {
						mailService.addTag(
								(GmailTag) db.getContent(DND_TAG_DATA_FORMAT)
								, (Set<GmailThread>) db.getContent(DND_THREADS_DATA_FORMAT));
						event.setDropCompleted(true);
						refreshSectionList();
						refreshThreadList();
					} catch (final Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				event.consume();
			});
			selectionPane.getChildren().add(sectionListPane);
		}

		/* thread list */ {
			threadListPane = new ThreadListPane<GmailSection, GmailTag, GmailThread>(mailService);
			threadListPane.setPrefWidth(250);
			threadListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			threadListPane.addSelectionListener((ov, oldVal, newVal) -> {
				if (!threadListPane.isRefreshing()) {
					// update movie details when (a) movie(s) is/are selected
					refreshThread();
				}
			});
			threadListPane.addChangeListener(change -> {
				// update movie count when a new movie list is loaded
				updateThreadCount();
				if (!threadListPane.isRefreshing()) {
					// update movie details in case no movie is selected
					refreshThread();
				}
			});
			threadListPane.setOnThreadDragDetected(event -> {
				final Set<GmailThread> threads = threadListPane.getSelectedMovies();
				if (threads.size() == 0) {
					return;
				}

		        final ClipboardContent content = new ClipboardContent();
		        content.put(DND_THREADS_DATA_FORMAT, threads);
		        final Dragboard db = threadListPane.startDragAndDrop(TransferMode.ANY);
		        db.setContent(content);
			});

			selectionPane.getChildren().add(threadListPane);
		}

		/* movie panel */ {
			threadPane = new ThreadPane<GmailSection, GmailTag, GmailThread>(mailService);
			threadPane.setOnDelTag(event -> {
				refreshSectionList();
				refreshThreadList();
			});
			/*
			moviePane.prefHeightProperty().bind(mainPane.heightProperty());
			new Callback<T, Void>() {
			@Override
			public Void call(final T tag) {
				try {
					mailService.remTag(tag, thread);
				} catch (final MailServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		}
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
//		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Movies.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void refreshSectionList() {
		sectionListPane.refresh();
		threadPane.refreshTags();
	}

	private void refreshThread() {
		final Set<GmailThread> selectedThreads = threadListPane.getSelectedMovies();
		threadPane.refresh(selectedThreads);
	}

	private void initData() {
        sectionListPane.refresh();
	}

	private void updateThreadCount() {
		final int currentCount = threadListPane.getThreadCount();
		Set<GmailTag> availableTags;
		try {
			availableTags = mailService.getTags();
		} catch (final TagServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		final String namePattern = "";
		sectionListPane.updateCount(currentCount, availableTags, namePattern);
	}

	private void refreshThreadList() {
		threadListPane.refreshWithTags(sectionListPane.getAllTags(), sectionListPane.getIncludedTags(), sectionListPane.getExcludedTags());
	}
}
