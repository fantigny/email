package net.anfoya.mail.browser.javafx.entrypoint;

import static net.anfoya.mail.browser.javafx.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import javax.security.auth.login.LoginException;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.ThreadListPane;
import net.anfoya.mail.browser.javafx.ThreadPane;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.javafx.scene.control.SectionListPane;
import net.anfoya.tag.javafx.scene.control.dnd.DndFormat;

public class MailBrowserApp extends Application {
//	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowserApp.class);

	public static void main(final String[] args) {
		launch(args);
	}

	private SectionListPane<GmailSection, GmailTag> sectionListPane;
	private MailService<GmailSection, GmailTag, GmailThread, SimpleMessage> mailService;
	private ThreadListPane<GmailSection, GmailTag, GmailThread> threadListPane;
	private ThreadPane<GmailTag, GmailThread, SimpleMessage> threadPane;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.setOnCloseRequest(event -> ThreadPool.getInstance().shutdown());

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
				updateThreadCount();
				return null;
			});
			sectionListPane.setOnDragDropped(event -> {
				final Dragboard db = event.getDragboard();
				if (db.hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT)
						&& db.hasContent(DndFormat.TAG_DATA_FORMAT)) {
					@SuppressWarnings("unchecked")
					final Set<GmailThread> threads = (Set<GmailThread>) db.getContent(DND_THREADS_DATA_FORMAT);
					final GmailTag tag = (GmailTag) db.getContent(DndFormat.TAG_DATA_FORMAT);
					addTag(tag, threads);
					event.setDropCompleted(true);
					event.consume();
				}
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

	private void addTag(final GmailTag tag, final Set<GmailThread> threads) {
		try {
			mailService.addForThreads(tag, threads);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		refreshSectionList();
		refreshThreadList();
	}

	private void initData() {
		sectionListPane.refresh();
		sectionListPane.selectTag(GmailSection.GMAIL_SYSTEM, "INBOX");
		sectionListPane.expand(GmailSection.GMAIL_SYSTEM);
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
