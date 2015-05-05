package net.anfoya.mail.browser.javafx.entrypoint;

import java.util.Set;

import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.security.auth.login.LoginException;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.gmail.GmailImpl;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.javafx.scene.control.SectionListPane;
import net.anfoya.tag.service.TagServiceException;

public class MailBrowserApp extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	private SectionListPane<GmailSection, GmailTag> sectionListPane;
	private MailService<GmailSection, GmailTag> mailService;
	private ListView<Thread> threadList;
	private WebEngine engine;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(final WindowEvent event) {
				ThreadPool.getInstance().shutdown();
			}
		});

		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) throws MailServiceException, LoginException {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(5));

		final Scene scene = new Scene(mainPane, 800, 600);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());

		final HBox selectionPane = new HBox();
		mainPane.setLeft(selectionPane);

		/* tag list */ {

			mailService = new GmailImpl();
			mailService.login(null, null);
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
			threadList = new ListView<Thread>();
			threadList.setPrefWidth(250);
			threadList.getSelectionModel().selectedItemProperty().addListener((ov, oldVal, newVal) -> {
				refreshThread(newVal);
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
			threadList.getItems().addListener((ListChangeListener<Thread>) change -> updateThreadCount());

			selectionPane.getChildren().add(threadList);
		}

		/* movie panel */ {
			final BorderPane mailPane = new BorderPane();
			final WebView view = new WebView();
			mailPane.setCenter(view);
			engine = view.getEngine();
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
			mainPane.setCenter(mailPane);
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

	private void refreshThread(final Thread thread) {
		engine.loadContent("");
		if (thread == null) {
			return;
		}
		try {
			final MimeMessage message = mailService.getMessage(thread.getMessageIds().iterator().next());
			engine.loadContent(message.getSubject());
		} catch (MailServiceException | MessagingException e) {
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
		try {
			final Set<? extends Thread> threads = mailService.getThreads(sectionListPane.getAllTags(), sectionListPane.getIncludedTags(), sectionListPane.getExcludedTags());
			threadList.getItems().setAll(threads);
		} catch (final MailServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
}
