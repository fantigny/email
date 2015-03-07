package net.anfoya.movies.javafx.movie;

import java.util.Set;
import java.util.LinkedHashSet;

import net.anfoya.movies.model.Movie;
import net.anfoya.movies.model.Tag;
import net.anfoya.movies.service.TagService;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.TilePane;

public class MovieTagsPane extends TitledPane {
	private final TagService tagService;

	private final TilePane tilePane;
	private final EventHandler<ActionEvent> checkBoxEventHandler = new EventHandler<ActionEvent>() {
		@Override
		public void handle(final ActionEvent event) {
			updateMovies(event);
		}
	};

	private Set<Movie> movies;
	private EventHandler<ActionEvent> addTagHandler;
	private EventHandler<ActionEvent> delTagHandler;

	public MovieTagsPane(final TagService tagService) {
		this.tagService = tagService;
		this.movies = new LinkedHashSet<Movie>();

		this.tilePane = new TilePane();
		this.tilePane.setTileAlignment(Pos.BASELINE_LEFT);
		this.tilePane.setStyle("-fx-background-color: #DDDDDD;");
		this.tilePane.setPadding(new Insets(5, 5, 5, 5));
		this.tilePane.setVgap(3);
		this.tilePane.setHgap(5);

		setText("Customize");
		setContent(tilePane);
	}

	public void refresh() {
		refresh(movies);
	}

	public void refresh(final Set<Movie> movies) {
		clear();
		this.movies = movies;
		if (movies.isEmpty()) {
			return;
		}

		// get common tags
		final Set<Tag> moviesTags = new LinkedHashSet<Tag>();
		for(final Movie movie: movies) {
			moviesTags.addAll(movie.getTags());
		}
		final Set<Tag> commonTags = new LinkedHashSet<Tag>(moviesTags);
		for(final Tag tag: moviesTags) {
			for(final Movie movie: movies) {
				if (!movie.getTags().contains(tag)) {
					commonTags.remove(tag);
				}
			}
			if (commonTags.isEmpty()) {
				break;
			}
		}

		final Set<Tag> allTags = tagService.getAllTags();
		for(final Tag tag: allTags) {
			final CheckBox checkBox = new CheckBox(tag.getName());
			checkBox.setOnAction(checkBoxEventHandler);
			if (commonTags.contains(tag)) {
				checkBox.setSelected(true);
			} else if (moviesTags.contains(tag)) {
				checkBox.setIndeterminate(true);
			}
			tilePane.getChildren().add(checkBox);
		}
	}

	private void updateMovies(final ActionEvent event) {
		final CheckBox checkBox = (CheckBox) event.getSource();
		final Tag tag = tagService.findOrCreate(checkBox.getText());
		final boolean add = checkBox.isSelected();

		final Set<Movie> toUpdateMovies = new LinkedHashSet<Movie>();
		for(final Movie movie: movies) {
			if (add && !movie.getTags().contains(tag)) {
				toUpdateMovies.add(movie);
			} else if (!add && movie.getTags().contains(tag)) {
				toUpdateMovies.add(movie);
			}
		}

		if (add) {
			tagService.addTagForMovies(tag, toUpdateMovies);
			addTagHandler.handle(event);
		} else {
			tagService.delTagForMovies(tag, toUpdateMovies, true);
			delTagHandler.handle(event);
		}
	}

	public void clear() {
		tilePane.getChildren().clear();
	}

	public void setOnAddTag(final EventHandler<ActionEvent> eventHandler) {
		this.addTagHandler = eventHandler;
	}

	public void setOnDelTag(final EventHandler<ActionEvent> eventHandler) {
		this.delTagHandler = eventHandler;
	}
}
