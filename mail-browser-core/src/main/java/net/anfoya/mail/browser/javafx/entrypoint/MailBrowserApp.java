package net.anfoya.mail.browser.javafx.entrypoint;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.security.auth.login.LoginException;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.browser.javafx.ThreadPane;
import net.anfoya.mail.gmail.GmailImpl;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.javafx.scene.control.SectionListPane;
import net.anfoya.tag.service.TagServiceException;

public class MailBrowserApp extends Application {
//	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowserApp.class);

	public static void main(final String[] args) {
		launch(args);
	}

	private SectionListPane<GmailSection, GmailTag> sectionListPane;
	private MailService<GmailSection, GmailTag, GmailThread> mailService;
	private ListView<GmailThread> threadList;
	private ThreadPane threadPane;

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
			sectionListPane.setTagChangeListener((ov, oldVal, newVal) -> refreshThreadList());
			sectionListPane.setUpdateSectionCallback(v -> {
				updateThreadCount();
				return null;
			});
			selectionPane.getChildren().add(sectionListPane);
		}

		/* thread list */ {
			final BorderPane threadListPane = new BorderPane();
			threadListPane.setPadding(new Insets(5, 0, 5, 0));

			threadList = new ListView<GmailThread>();
			threadList.setPrefWidth(250);
			threadList.getSelectionModel().selectedItemProperty().addListener((ov, oldVal, newVal) -> {
				threadPane.load(newVal);
			});
			/*
			movieListPane.addSelectionListener(new ChangeListener<Movie>() {
				@Override
				public void changed(final ObservableValue<? extends Movie> ov, final Movie oldVal, final Movie newVal) {
					if (!movieListPane.isRefreshing()) {
						// update movie details when (a) movie(s) is/are selected
						refreshMovie();
					}
				}
			});
			*/
			threadList.getItems().addListener((ListChangeListener<SimpleThread>) change -> {
				updateThreadCount();
			});
			threadListPane.setCenter(threadList);

			selectionPane.getChildren().add(threadListPane);
		}

		/* movie panel */ {
			threadPane = new ThreadPane(mailService);
			/*
			moviePane.prefHeightProperty().bind(mainPane.heightProperty());
			moviePane.setOnAddTag(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					refreshSectionList();
					refreshMovieList();
				}
			});
			moviePane.setOnDelTag(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					refreshSectionList();
					refreshMovieList();
				}
			});
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

	private void initData() {
        sectionListPane.refresh();
		try {
			sectionListPane.updateCount(1, mailService.getTags(), "");
		} catch (final TagServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void updateThreadCount() {
		final int currentCount = threadList.getItems().size();
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
		threadList.getItems().clear();
		final Task<Set<GmailThread>> task = new Task<Set<GmailThread>>() {
			@Override
			protected Set<GmailThread> call() throws Exception {
				final ExecutorService service = Executors.newFixedThreadPool(10);
				final Set<Future<GmailThread>> tasks = new LinkedHashSet<Future<GmailThread>>();
				for(final String id: mailService.getThreadIds(sectionListPane.getAllTags(), sectionListPane.getIncludedTags(), sectionListPane.getExcludedTags())) {
					final Callable<GmailThread> c = new Callable<GmailThread>() {
						@Override
						public GmailThread call() throws Exception {
							return mailService.getThread(id);
						}
					};
					tasks.add(service.submit(c));
				}
				final Set<GmailThread> threads = new LinkedHashSet<GmailThread>();
				for(final Future<GmailThread> f: tasks) {
					threads.add(f.get());
				}
				service.shutdown();
				return threads;
			}
		};
		task.setOnSucceeded(event -> {
			try {
				threadList.getItems().setAll(task.get());
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		ThreadPool.getInstance().submit(task);
	}
}
