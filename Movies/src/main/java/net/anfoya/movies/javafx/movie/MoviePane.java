package net.anfoya.movies.javafx.movie;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import net.anfoya.movies.model.Movie;
import net.anfoya.movies.model.MovieWebsite;
import net.anfoya.movies.model.Profile;
import net.anfoya.movies.model.Tag;
import net.anfoya.movies.service.MovieService;
import net.anfoya.movies.service.TagService;
import net.anfoya.tools.javafx.scene.control.Title;
import net.anfoya.tools.javafx.scene.control.TitledProgressBar;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;


public class MoviePane extends TitledPane {
	private static final String NO_MOVIE_TITLE = "Select a movie to see details";
	private static final String MULTIPLE_MOVIE_TITLE = "Multiple movies";

	private Set<Movie> movies;

	private final VBox movieBox;

	private final Title title;
	private final TabPane webPanes;
	private final MovieToolboxPane toolboxPane;
	private final MovieTagsPane tagsPane;

	public MoviePane(final MovieService movieService, final TagService tagService, final Profile profile) {
		this.movies = new LinkedHashSet<Movie>();

		title = new Title(NO_MOVIE_TITLE);

		setCollapsible(false);
		setGraphic(title);

		final BorderPane borderPane = new BorderPane();
		setContent(borderPane);

		// web pages
		webPanes = new TabPane();
		webPanes.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		webPanes.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
		borderPane.setCenter(webPanes);

		// tags
		tagsPane = new MovieTagsPane(tagService);
		tagsPane.setExpanded(false);

		// tool box
		toolboxPane = new MovieToolboxPane(movieService, tagService, webPanes);
		toolboxPane.advancedProperty().bind(tagsPane.expandedProperty());

		// box is displayed when at least a movie is selected
		if (profile != Profile.RESTRICTED) {
			movieBox = new VBox(toolboxPane, tagsPane);
		} else {
			movieBox = new VBox(toolboxPane);
		}

		// initialize web tabs
		for(final MovieWebsite website: MovieWebsite.LIST) {
			final MovieWebPane webPanel = new MovieWebPane(website);
			webPanel.setOnSavePage(toolboxPane.getOnSavePage());

			final TitledProgressBar progressTitle = new TitledProgressBar(website.getName());
			progressTitle.setPrefWidth(120);
			progressTitle.progressProperty().bind(webPanel.progressProperty());
			progressTitle.stateProperty().bind(webPanel.stateProperty());

			final Tab tab = new Tab();
			tab.setGraphic(progressTitle);
			tab.setContent(webPanel);
			tab.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
					if (newVal) {
						webPanel.load(movies, tab.isSelected());
					}
				}
			});

			webPanes.getTabs().add(tab);
		}
	}

	public void load(final Set<Movie> movies) {
		this.movies = movies;

		loadWebPane();
		updateTitle();

		toolboxPane.refresh(movies);

		tagsPane.refresh(movies);

		setDisplayMovieBox(!movies.isEmpty());
	}

	public void refreshTags() {
		tagsPane.refresh();
	}

	public void setOnAddTag(final EventHandler<ActionEvent> handler) {
		tagsPane.setOnAddTag(handler);
	}

	public void setOnDelTag(final EventHandler<ActionEvent> handler) {
		tagsPane.setOnDelTag(handler);
	}

	public void setOnCreateTag(final EventHandler<ActionEvent> handler) {
		toolboxPane.setOnCreateTag(handler);
	}

	public void setOnUpdateMovie(final EventHandler<ActionEvent> handler) {
		toolboxPane.setOnUpdateMovie(handler);
	}

	protected TabPane getWebPanes() {
		return webPanes;
	}

	protected MovieToolboxPane getToolboxPane() {
		return toolboxPane;
	}

	protected MovieTagsPane getTagsPane() {
		return tagsPane;
	}

	private void loadWebPane() {
		webPanes.getTabs().forEach(new Consumer<Tab>() {
			@Override
			public void accept(final Tab tab) {
				final MovieWebPane webPanel = (MovieWebPane) tab.getContent();
				webPanel.load(movies, tab.isSelected());
			}
		});
	}

	private void updateTitle() {
		String main;
		String sub = "";
		switch (movies.size()) {
		case 0:
			main = NO_MOVIE_TITLE;
			break;
		case 1:
			final Movie movie = movies.iterator().next();
			main = movie.getName();
			final StringBuilder sb = new StringBuilder("  =  ");
			for(final Tag tag: movie.getTags()) {
				final String tagName = tag.getName();
				if (!tagName.equals(Tag.NO_TAG_NAME)
						&& !tagName.equals(Tag.TO_WATCH_NAME)) {
					sb.append(tagName);
					sb.append("  +  ");
				}
			}
			sub = sb.substring(0, sb.length() - 5);
			break;
		default:
			main = MULTIPLE_MOVIE_TITLE;
			break;
		}

		title.set(main, sub);
	}

	private void setDisplayMovieBox(final boolean display) {
		final BorderPane borderPane = (BorderPane) getContent();
		if (display) {
			borderPane.setBottom(movieBox);
		} else {
			borderPane.getChildren().remove(movieBox);
		}
	}
}
