package net.anfoya.mail.browser.javafx.entrypoint;

import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import net.anfoya.mail.gmail.GmailImpl;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.mail.tag.TagServiceImpl;
import net.anfoya.tag.javafx.scene.control.SectionListPane;
import net.anfoya.tag.model.Section;
import net.anfoya.tag.service.TagServiceException;

public class MailBrowserApp extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	private SectionListPane sectionListPane;
	private TagServiceImpl tagService;
	private MailService mailService;
	private ListView<Thread> threadList;
	private WebEngine engine;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) throws MailServiceException {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(5));

		final Scene scene = new Scene(mainPane, 800, 600);

		final HBox selectionPane = new HBox();
		mainPane.setLeft(selectionPane);

		/* tag list */ {

			mailService = new GmailImpl();
			mailService.login(null, null);
			tagService = new TagServiceImpl(mailService);
			sectionListPane = new SectionListPane(tagService);
			sectionListPane.setPrefWidth(250);
			sectionListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			sectionListPane.setTagChangeListener((ov, oldVal, newVal) -> refreshThreadList());
			sectionListPane.setUpdateSectionCallback(v -> {
				updateThreadCount();
				return null;
			});
			selectionPane.getChildren().add(sectionListPane);
		}

		/* movie list */ {
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
			movieListPane.addChangeListener(new ListChangeListener<Movie>() {
				@Override
				public void onChanged(final ListChangeListener.Change<? extends Movie> change) {
					// update movie count when a new movie list is loaded
					updateMailCount();
					if (!movieListPane.isRefreshing()) {
						// update movie details in case no movie is selected
						refreshMovie();
					}
				}

				private void updateMailCount() {
					// TODO Auto-generated method stub

				}
			});
			*/
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
			sectionListPane.updateCount(1, tagService.getTags(Section.NO_SECTION, ""), "");
		} catch (final TagServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void refreshThread(final Thread thread) {
		engine.load(mailService.getMail(thread.getMailIds().get(0)));
	}

	private void updateThreadCount() {
		// TODO Auto-generated method stub
	}

	private void refreshThreadList() {
		List<Thread> threads;
		try {
			threads = mailService.getThreads(sectionListPane.getAllSelectedTags());
		} catch (final MailServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		threadList.getItems().clear();
		for(final Thread t: threads) {
			threadList.getItems().add(t);
		}
	}
}
