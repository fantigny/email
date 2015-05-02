package net.anfoya.mail.browser.javafx.entrypoint;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

public class MailBrowserApp extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(5));

		final Scene scene = new Scene(mainPane, 800, 600);

		final HBox selectionPane = new HBox();
		mainPane.setLeft(selectionPane);

		/* tag list */ {

			sectionListPane.setPrefWidth(250);
			sectionListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			sectionListPane.setTagChangeListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
					// refresh movie list when a tag is (un)selected
					refreshMovieList();
				}
			});
			sectionListPane.setUpdateSectionCallback(new Callback<Void, Void>() {
				@Override
				public Void call(final Void v) {
					updateMovieCount();
					return null;
				}
			});
			if (profile != Profile.RESTRICTED) {
				selectionPane.getChildren().add(sectionListPane);
			}
		}

		/* movie list */ {
			movieListPane.setPrefWidth(250);
			movieListPane.prefHeightProperty().bind(selectionPane.heightProperty());
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
					updateMovieCount();
					if (!movieListPane.isRefreshing()) {
						// update movie details in case no movie is selected
						refreshMovie();
					}
				}
			});

			selectionPane.getChildren().add(movieListPane);
		}

		/* movie panel */ {
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

			mainPane.setCenter(moviePane);
		}

		primaryStage.setTitle("FisherMail / Agaar / Agamar / Agaram");
//		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Movies.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void initData() {
		// TODO Auto-generated method stub

	}
}
