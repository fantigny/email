package net.anfoya.downloads.javafx.allocine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class AllocineField extends ComboBox<AllocineMovie> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineField.class);
	private static final String SEARCH_PATTERN = "http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=%s";

	private volatile AllocineMovie searchedMovie;

	private volatile Future<?> autocompFuture;
	private volatile AllocineMovie lastAutocompMovie;

	public AllocineField() {
		setEditable(true);
		setButtonCell(new AllocineListCell());
		setCellFactory(new Callback<ListView<AllocineMovie>, ListCell<AllocineMovie>>() {
			@Override
			public ListCell<AllocineMovie> call(final ListView<AllocineMovie> movie) {
				return new AllocineListCell();
			}
		});

		searchedMovie = AllocineMovie.getEmptyMovie();
		autocompFuture = null;
		lastAutocompMovie = AllocineMovie.getEmptyMovie();

		setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				final AllocineMovie newVal = getValue();
				if (!isShowing() && !searchedMovie.equals(newVal)) {
					updateList(newVal);
				}
			}
		});
		getEditor().textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> observable,
					final String oldValue, final String newValue) {
				setValue(new AllocineMovie(newValue));
			}
		});

		setConverter(new StringConverter<AllocineMovie>() {
			@Override
			public String toString(final AllocineMovie movie) {
				return movie == null
						? ""
						: movie.toString();
			}

			@Override
			public AllocineMovie fromString(final String string) {
				return string == null
						? AllocineMovie.getEmptyMovie()
						: new AllocineMovie(string);
			}
		});
	}

	private void updateList(final AllocineMovie movie) {
		if (movie == null) {
			hide();
			getItems().clear();
			return;
		}

		if (lastAutocompMovie.equals(movie)) {
			// list is already loaded
			if (!getItems().isEmpty() && !isShowing()) {
				show();
			}
			return;
		}
		lastAutocompMovie = movie;

		final String title = movie.toString();
		if (title.length() < 3) {
			// need more characters
			return;
		}

		if (isShowing()) {
			hide();
		}

		requestList(title);
	}

	private void requestList(final String title) {
		final String url;
		try {
			url = String.format(SEARCH_PATTERN, URLEncoder.encode(title, "UTF8"));
		} catch (final UnsupportedEncodingException e) {
			LOGGER.error("building url {}", String.format(SEARCH_PATTERN, title), e);
			return;
		}
		if (autocompFuture != null) {
			autocompFuture.cancel(true);
		}
		autocompFuture = ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(500); // allow user to type more characters
				} catch (final InterruptedException e) {
				} finally {
					if (autocompFuture.isCancelled()) {
						LOGGER.info("cancelled {}", url);
						return;
					}
				}
				LOGGER.info("requested {}", url);
				final List<AllocineMovie> movies = new ArrayList<AllocineMovie>();
				try {
					final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
					final JsonArray jsonMovies = new JsonParser().parse(reader).getAsJsonArray();
					jsonMovies.forEach(new Consumer<JsonElement>() {
						@Override
						public void accept(final JsonElement element) {
							final AllocineMovie movie = new AllocineMovie(element.getAsJsonObject());
							if (!movie.getThumbnail().isEmpty()) {
								movies.add(movie);
							}
						}
					});
				} catch (final Exception e) {
					LOGGER.error("loading {}", url, e);
				}

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						if (autocompFuture.isCancelled()) {
							LOGGER.info("cancelled {}", url);
							return;
						}
						getItems().clear();
						getItems().addAll(movies);
						if (!getItems().isEmpty()) {
							show();
						}
					}
				});
			}
		});
	}

	public void setSearchedText(final String searched) {
		final AllocineMovie searchedMovie = new AllocineMovie(searched);
		if (!searchedMovie.equals(this.searchedMovie)) {
			this.searchedMovie = searchedMovie;
			setValue(searchedMovie);
		}
	}
/*
	public String getSearch() {
		AllocineMovie movie = getValue();
		if (movie == null) {
			final String text = getEditor().getText();
			if (text != null) {
				movie = new AllocineMovie(text);
			}
		}
		if (movie == null) {
			movie = AllocineMovie.getEmptyMovie();
		}
		return movie.toString();
	}
*/
	public void setOnSearch(final Callback<String[], Void> callback) {
		addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				callback.call(new String[] { getValue().toString() });
			}
		});
	}
}
