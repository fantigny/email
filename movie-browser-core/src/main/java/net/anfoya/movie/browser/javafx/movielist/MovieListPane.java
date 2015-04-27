package net.anfoya.movie.browser.javafx.movielist;

import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import net.anfoya.javafx.scene.control.Title;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Movie.SortOrder;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.movie.browser.service.MovieService;

public class MovieListPane extends BorderPane {

	private final MovieList movieList;
	private final TextField namePatternField;

	public MovieListPane(final MovieService movieService) {
		final BorderPane patternPane = new BorderPane();
		setTop(patternPane);

		final Title title = new Title("Movies");
		title.setPadding(new Insets(0, 10, 0, 5));
		patternPane.setLeft(title);

		namePatternField = new TextField();
		namePatternField.setPromptText("search");
		namePatternField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> ov, final String oldPattern, final String newPattern) {
				movieList.refreshWithPattern(newPattern);
			}
		});
		patternPane.setCenter(namePatternField);
		BorderPane.setMargin(namePatternField, new Insets(0, 5, 0, 5));

		final Button delPatternButton = new Button("X");
		delPatternButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				namePatternField.textProperty().set("");
			}
		});
		patternPane.setRight(delPatternButton);

		movieList = new MovieList(movieService);
		setCenter(movieList);

		final ToggleGroup toggleGroup = new ToggleGroup();

		final RadioButton nameSortButton = new RadioButton("name ");
		nameSortButton.setToggleGroup(toggleGroup);

		final RadioButton dateSortButton = new RadioButton("date");
		dateSortButton.setToggleGroup(toggleGroup);

		toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(final ObservableValue<? extends Toggle> ov, final Toggle oldVal, final Toggle newVal) {
				movieList.refreshWithOrder(nameSortButton.isSelected()
						? SortOrder.NAME
						: SortOrder.DATE);
			}
		});
		dateSortButton.setSelected(true);

		final HBox box = new HBox(new Label("Sort by: "), nameSortButton, dateSortButton);
		box.setAlignment(Pos.BASELINE_CENTER);
		box.setSpacing(5);
		setBottom(box);

		setMargin(patternPane, new Insets(5));
		setMargin(movieList, new Insets(0, 5, 0, 5));
		setMargin(box, new Insets(5));
	}

	public String getNamePattern() {
		return namePatternField.getText();
	}

	public void refreshWithTags(final Set<Tag> tags, final Set<Tag> includes, final Set<Tag> excludes) {
		movieList.refreshWithTags(tags, includes, excludes);
	}

	public int getMovieCount() {
		return movieList.getItems().size();
	}

	public Set<Tag> getMoviesTags() {
		return movieList.getMoviesTags();
	}

	public Set<Movie> getSelectedMovies() {
		return movieList.getSelectedMovies();
	}

	public boolean isRefreshing() {
		return movieList.isRefreshing();
	}

	public ObservableList<Movie> getItems() {
		return movieList.getItems();
	}

	public void addSelectionListener(final ChangeListener<Movie> listener) {
		movieList.getSelectionModel().selectedItemProperty().addListener(listener);
	}

	public void addChangeListener(final ListChangeListener<Movie> listener) {
		movieList.getItems().addListener(listener);
	}
}
