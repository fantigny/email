package net.anfoya.movie.browser.javafx.movielist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Movie.SortOrder;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.movie.browser.service.MovieService;

//TODO: sort by name when all titles start with a numeric

public class MovieList extends ListView<Movie> {
	private final MovieService movieService;

	private final Predicate<Movie> nameFilter = new Predicate<Movie>() {
		@Override
		public boolean test(final Movie movie) {
			return movie.getName().toLowerCase().contains(namePattern);
		}
	};

	private boolean refreshing;
	private Set<Movie> movies;

	private Set<Tag> tags;
	private Set<Tag> includes;
	private Set<Tag> excludes;
	private SortOrder sortOrder;
	private String namePattern;

	public MovieList(final MovieService movieService) {
		this.movieService = movieService;

		this.refreshing = false;
		this.movies = new LinkedHashSet<Movie>();

		this.tags = new LinkedHashSet<Tag>();
		this.includes = new LinkedHashSet<Tag>();
		this.excludes = new LinkedHashSet<Tag>();
		this.sortOrder = SortOrder.DATE;
		this.namePattern = "";

		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	public void refreshWithPattern(final String pattern) {
		final String previousPattern = namePattern;
		namePattern = pattern.toLowerCase();

		if (!namePattern.contains(previousPattern)) {
			movies = movieService.getMovies(tags, includes, excludes, namePattern);
		}
		refresh();
	}

	public void refreshWithOrder(final SortOrder order) {
		sortOrder = order;
		refresh();
	}

	public void refreshWithTags(final Set<Tag> tags, final Set<Tag> includes, final Set<Tag> excludes) {
		this.tags = tags;
		this.includes = includes;
		this.excludes = excludes;
		movies = movieService.getMovies(tags, includes, excludes, namePattern);
		refresh();
	}

	public void refresh() {
		// get previously selected indices
		final List<Integer> previouslySelectedIds = new ArrayList<Integer>();
		for(final Movie movie: getSelectedMovies()) {
			previouslySelectedIds.add(movie.getId());
		}

		// get list
		ObservableList<Movie> obsMovies = FXCollections.observableArrayList(movies);

		// filter
		obsMovies = FXCollections.observableArrayList(obsMovies.filtered(nameFilter));

		// sort
		Collections.sort(obsMovies, sortOrder.getComparator());

		// find selected indices in new list
		final int[] indices = new int[previouslySelectedIds.size()];
		Arrays.fill(indices, -1);
		int listIndex = 0, arrayIndex = 0;
		for(final Movie movie: obsMovies) {
			if (previouslySelectedIds.contains(movie.getId())) {
				indices[arrayIndex] = listIndex;
				arrayIndex++;
			}
			listIndex++;
		}

		// display
		refreshing = !previouslySelectedIds.isEmpty() && indices.length > 0 && indices[0] != -1;
		getItems().setAll(obsMovies);
		refreshing = false;

		// restore selection
		if (indices.length > 0 && indices[0] != -1) {
			getSelectionModel().selectIndices(indices[0], indices);
		}
	}

	public Set<Tag> getMoviesTags() {
		// return all tags available from all movies
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final Movie movie: getItems()) {
			tags.addAll(movie.getTags());
		}

		return Collections.unmodifiableSet(tags);
	}

	public boolean isRefreshing() {
		return refreshing;
	}

	public Set<Movie> getSelectedMovies() {
		final List<Movie> selectedMovies = getSelectionModel().getSelectedItems();
		if (!selectedMovies.isEmpty() && selectedMovies.iterator().next() == null) {
			return Collections.unmodifiableSet(new LinkedHashSet<Movie>());
		}
		return Collections.unmodifiableSet(new LinkedHashSet<Movie>(selectedMovies));
	}
}
