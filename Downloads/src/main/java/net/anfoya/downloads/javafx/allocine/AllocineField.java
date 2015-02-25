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

	private volatile AllocineMovie currentMovie;
	private volatile AllocineMovie searchedMovie;
	private volatile AllocineMovie requestMovie;

	private volatile Future<?> requestFuture;

	private Callback<AllocineMovie, Void> searchCallback;

	public AllocineField() {
		setEditable(true);
		setButtonCell(new AllocineListCell());
		setCellFactory(new Callback<ListView<AllocineMovie>, ListCell<AllocineMovie>>() {
			@Override
			public ListCell<AllocineMovie> call(final ListView<AllocineMovie> movie) {
				return new AllocineListCell();
			}
		});

		currentMovie = null;
		searchedMovie = null;
		requestFuture = null;
		requestMovie = null;

		addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				switch(event.getCode()) {
				case ENTER:
					submitSearch();
					break;
				default:
				}
			}
		});

		getEditor().addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				switch(event.getCode()) {
				case RIGHT: case LEFT: case UP:
					break;
				case DOWN:
					if (!isShowing()) {
						updateList();
					}
					break;
				default:
					updateList();
				}
			}
		});
		getEditor().textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> ov, final String oldVal, final String newVal) {
				currentMovie = new AllocineMovie(newVal);
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

	public void submitSearch() {
		if (searchCallback != null
				&& currentMovie != null
				&& !currentMovie.toString().isEmpty()
				&& !currentMovie.equals(searchedMovie)) {
			final AllocineMovie movie = getValue();
			if (currentMovie.equals(movie)) {
				currentMovie = movie;
			}
			searchedMovie = currentMovie;
			searchCallback.call(currentMovie);

			if (requestFuture != null) {
				requestFuture.cancel(true);
				hide();
			}
		}
	}

	private synchronized void updateList() {
		if (currentMovie == null) {
			// nothing to display
			getItems().clear();
			hide();
			return;
		}

		if (currentMovie.equals(requestMovie)
				&& requestFuture != null && requestFuture.isDone()) {
			// list was already requested
			if (!isShowing() && !getItems().isEmpty()) {
				show();
			}
			return;
		}
		requestMovie = currentMovie;

		requestList();
	}

	private synchronized void requestList() {
		final String title = requestMovie.toString();
		if (title.length() < 3) {
			// need more characters
			hide();
			if (requestFuture != null) {
				requestFuture.cancel(true);
			}
			return;
		}

		final String url;
		try {
			url = String.format(SEARCH_PATTERN, URLEncoder.encode(title, "UTF8"));
		} catch (final UnsupportedEncodingException e) {
			LOGGER.error("building url {}", String.format(SEARCH_PATTERN, title), e);
			return;
		}
		if (requestFuture != null) {
			requestFuture.cancel(true);
		}
		requestFuture = ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				final List<AllocineMovie> movies = new ArrayList<AllocineMovie>();
				try {
					Thread.sleep(500); // allow user to type more characters
					LOGGER.info("requested {}", url);
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
				} catch (final InterruptedException e) {
					return;
				} catch (final Exception e) {
					LOGGER.error("loading {}", url, e);
					return;
				}

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						LOGGER.info("displayed {}", url);
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

	public void setOnSearch(final Callback<AllocineMovie, Void> callback) {
		searchCallback = callback;
	}
}
