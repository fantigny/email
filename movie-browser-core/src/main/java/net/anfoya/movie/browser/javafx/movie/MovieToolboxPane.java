package net.anfoya.movie.browser.javafx.movie;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.util.Callback;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.movie.browser.model.Website;
import net.anfoya.movie.browser.service.MovieService;
import net.anfoya.movie.browser.service.TagService;

public class MovieToolboxPane extends TilePane {
	private final MovieService movieService;
	private final TagService tagService;

	private final Callback<MovieWebPane, Void> savePageCallback = new Callback<MovieWebPane, Void>() {
		@Override
		public Void call(final MovieWebPane movieWebPane) {
			savePage(movieWebPane.getWebsite().getName(), movieWebPane.getUrl(), null);
			return null;
		}
	};

	private final Button renameButton;
	private final Button savePageButton;

	private final TabPane tabWebPane;
	private final BooleanProperty advancedProperty;

	private EventHandler<ActionEvent> createTagHandler;
	private EventHandler<ActionEvent> updateMovieHandler;
	private Set<Movie> movies;


	public MovieToolboxPane(final MovieService movieService, final TagService tagService, final TabPane tabWebPane) {
		this.tagService = tagService;
		this.movieService = movieService;
		this.tabWebPane = tabWebPane;
		setAlignment(Pos.BASELINE_CENTER);
		setHgap(10);
		setPadding(new Insets(7, 0, 7, 0));
		setStyle("-fx-background-color: #555555;");

		savePageButton = new Button("Save page");
		savePageButton.setPrefSize(120, 20);
		savePageButton.setOnAction(event -> savePage(event));

		final Button createTagButton = new Button("Create tag");
		createTagButton.setPrefSize(120, 20);
		createTagButton.setOnAction(event -> createTag(event));

		final ImageView playIcon = new ImageView(new Image(getClass().getResourceAsStream("play.png")));
		final Button playButton = new Button("Play", playIcon);
		playButton.setGraphicTextGap(15);
		playButton.setPrefSize(120, 20);
		playButton.setOnAction(event -> playMovies());
		getChildren().add(playButton);

		final ImageView openFolderIcon = new ImageView(new Image(getClass().getResourceAsStream("open_folder.png")));
		final Button openFolderButton = new Button("Show file", openFolderIcon);
		openFolderButton.setPrefSize(120, 20);
		openFolderButton.setOnAction(event -> showMovieFiles());
		getChildren().add(openFolderButton);

		renameButton = new Button("Rename");
		renameButton.setPrefSize(120, 20);
		renameButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				renameMovie(event);
			}
		});

		final Button deleteButton = new Button("Delete");
		deleteButton.setPrefSize(120, 20);
		deleteButton.setOnAction(event -> deleteMovies(event));

		advancedProperty = new SimpleBooleanProperty(false);
		advancedProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean advanced) {
				if (advanced) {
					if (!getChildren().contains(savePageButton)) {
						getChildren().add(0, savePageButton);
						getChildren().add(1, createTagButton);
						getChildren().add(4, renameButton);
						getChildren().add(5, deleteButton);
					}
				} else {
					getChildren().remove(savePageButton);
					getChildren().remove(createTagButton);
					getChildren().remove(renameButton);
					getChildren().remove(deleteButton);
				}
			}
		});
	}

	public void refresh(final Set<Movie> movies) {
		this.movies = movies;
		refresh();
	}

	private void refresh() {
		renameButton.setDisable(movies.size() != 1);
		savePageButton.setDisable(movies.size() != 1);
		setVisible(true);
	}

	public void clear() {
		this.movies = null;
		setVisible(false);
	}

	private void playMovies() {
		for(final Movie movie: movies) {
			movieService.play(movie);
		}
	}

	private void showMovieFiles() {
		for(final Movie movie: movies) {
			movieService.showInFileMngr(movie);
		}
	}

	public Callback<MovieWebPane, Void> getOnSavePage() {
		return savePageCallback;
	}

	private void createTag(final ActionEvent event) {
		final TextInputDialog inputDialog = new TextInputDialog();
		inputDialog.setTitle("Create new tag");
		inputDialog.setContentText("Tag name:");
		inputDialog.setHeaderText("");
		final Optional<String> response = inputDialog.showAndWait();

		if (!response.isPresent()) {
			return;
		}
		final String tagName = response.get();
		if (tagName.length() < 3) {
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setTitle("Create new tag");
			alertDialog.setContentText("Tag name should be a least 3 letters long.");
			alertDialog.setHeaderText("Tag name is too short: " + tagName);
			alertDialog.showAndWait();
			return;
		}

		final Tag tag = tagService.findOrCreate(tagName);
		final Set<Movie> toAddTagMovies = new LinkedHashSet<Movie>();
		for(final Movie movie: movies) {
			if (!movie.getTags().contains(tag)) {
				toAddTagMovies.add(movie);
			}
		}

		tagService.addTagForMovies(tag, toAddTagMovies);
		createTagHandler.handle(event);
	}

	private void deleteMovies(final ActionEvent event) {
		final Alert confirmDialog = new Alert(AlertType.CONFIRMATION, movies.toString(), new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
		confirmDialog.setTitle("Delete movie" + (movies.isEmpty()? "": "s"));
		confirmDialog.setHeaderText("Do you want to delete");
		final Optional<ButtonType> response = confirmDialog.showAndWait();
		if (response.isPresent() && response.get() == ButtonType.OK) {
			movieService.delMovies(movies);
			updateMovieHandler.handle(event);
		}
	}

	private void renameMovie(final ActionEvent event) {
		final Movie movie = movies.iterator().next();
		final TextInputDialog inputDialog = new TextInputDialog(movie.getName().replace(".", " "));
		inputDialog.setTitle("Rename movie");
		inputDialog.setContentText("New name for movie:");
		inputDialog.setHeaderText("");
		final Optional<String> response = inputDialog.showAndWait();

		if (response.isPresent()) {
			final String name = response.get();
			if (!name.equals(movie.getName())) {
				if (name.length() > 2) {
					movieService.rename(movie, name);
					updateMovieHandler.handle(event);
				} else {
					final Alert error = new Alert(AlertType.ERROR);
					error.setTitle("Rename movie");
					error.setContentText("New name for movie \"" + name + "\" is to short (less than 3 letters)");
					error.setHeaderText("");
					error.showAndWait();
				}
			}
		}
	}

	private void savePage(final ActionEvent event) {
		final MovieWebPane movieWebPane = (MovieWebPane) tabWebPane.getSelectionModel().getSelectedItem().getContent();
		final Website website = movieWebPane.getWebsite();
		final String url = movieWebPane.getUrl();

		savePage(website.getName(), url, event);
	}

	private void savePage(final String websiteName, final String url, final ActionEvent event) {
		movieService.saveMovieUrls(movies.iterator().next(), websiteName, url);
		updateMovieHandler.handle(event);
	}

	public void setOnUpdateMovie(final EventHandler<ActionEvent> eventHandler) {
		this.updateMovieHandler = eventHandler;
	}

	public void setOnCreateTag(final EventHandler<ActionEvent> eventHandler) {
		this.createTagHandler = eventHandler;
	}

	public BooleanProperty advancedProperty() {
		return advancedProperty;
	}
}
