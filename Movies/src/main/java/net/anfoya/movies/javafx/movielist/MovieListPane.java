package net.anfoya.movies.javafx.movielist;

import java.util.Set;

import net.anfoya.movies.model.Movie;
import net.anfoya.movies.model.Tag;
import net.anfoya.movies.model.Movie.SortOrder;
import net.anfoya.movies.service.MovieService;
import net.anfoya.tools.javafx.scene.control.Title;
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
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class MovieListPane extends TitledPane {

	private final MovieList movieList;
	private final TextField namePatternField;

	public MovieListPane(final MovieService movieService) {
		setGraphic(new Title("Movies"));
		setCollapsible(false);

		final BorderPane borderPane = new BorderPane();
		setContent(borderPane);

		final BorderPane patternPane = new BorderPane();
		patternPane.setPadding(new Insets(0, 0, 5, 0));
		borderPane.setTop(patternPane);

		namePatternField = new TextField();
		BorderPane.setMargin(namePatternField, new Insets(0, 3, 0, 3));
		namePatternField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> ov, final String oldPattern, final String newPattern) {
				movieList.refreshWithPattern(newPattern);
			}
		});
		patternPane.setCenter(namePatternField);

		final Button delPatternButton = new Button("X");
		delPatternButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				namePatternField.textProperty().set("");
			}
		});
		patternPane.setRight(delPatternButton);

		final Label searchLabel = new Label("Search:");
		BorderPane.setAlignment(searchLabel, Pos.CENTER);
		patternPane.setLeft(searchLabel);

		movieList = new MovieList(movieService);
		borderPane.setCenter(movieList);

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
		box.setPadding(new Insets(7, 0, 0, 0));
		borderPane.setBottom(box);
	}

	public String getNamePattern() {
		return namePatternField.getText();
	}

	public void refreshWithTags(final Set<Tag> tags) {
		movieList.refreshWithTags(tags);
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
