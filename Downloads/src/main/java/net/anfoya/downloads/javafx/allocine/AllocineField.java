package net.anfoya.downloads.javafx.allocine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

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

public class AllocineField extends ComboBox<AllocineQsResult> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineField.class);
	private static final String SEARCH_PATTERN = "http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=%s";

	private volatile AllocineQsResult currentQs;
	private volatile AllocineQsResult searchedQs;
	private volatile AllocineQsResult requestQs;

	private volatile Future<?> requestFuture;

	private Callback<AllocineQsResult, Void> searchCallback;

	public AllocineField() {
		setEditable(true);
		setPromptText("Key in a text and wait for quick search or type <Enter> for full search");
		setButtonCell(new AllocineQsListCell());
		setCellFactory(new Callback<ListView<AllocineQsResult>, ListCell<AllocineQsResult>>() {
			@Override
			public ListCell<AllocineQsResult> call(final ListView<AllocineQsResult> listView) {
				return new AllocineQsListCell();
			}
		});

		currentQs = null;
		searchedQs = null;
		requestFuture = null;
		requestQs = null;

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

		getEditor().addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
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

		getEditor().addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				switch(event.getCode()) {
				case RIGHT: case LEFT: case UP:
					break;
				case DOWN:
					if (!isShowing()) {
						showQuickSearch();
					}
					break;
				default:
					showQuickSearch();
				}
			}
		});
		getEditor().textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> ov, final String oldVal, final String newVal) {
				currentQs = new AllocineQsResult(newVal);
			}
		});

		setConverter(new StringConverter<AllocineQsResult>() {
			@Override
			public String toString(final AllocineQsResult qsResult) {
				return qsResult == null? "": qsResult.toString();
			}
			@Override
			public AllocineQsResult fromString(final String string) {
				return string == null? AllocineQsResult.getEmptyResult(): new AllocineQsResult(string);
			}
		});
	}

	private void submitSearch() {
		cancelQuickSearch();
		if (searchCallback != null
				&& currentQs != null
				&& !currentQs.toString().isEmpty()
				&& !currentQs.equals(searchedQs)) {
			final AllocineQsResult qs = getValue();
			if (currentQs.equals(qs)) {
				currentQs = qs;
			}
			searchedQs = currentQs;
			searchCallback.call(currentQs);
		}
	}

	private void cancelQuickSearch() {
		hide();
		if (requestFuture != null
				&& !requestFuture.isDone()
				&& !requestFuture.isCancelled()) {
			requestFuture.cancel(true);
		}
	}

	private synchronized void showQuickSearch() {
		if (currentQs == null) {
			// nothing to display
			cancelQuickSearch();
			return;
		}

		if (currentQs.equals(searchedQs)) {
			if (!isShowing()) {
				//TODO check
				// loading a movie in browser
				return;
			}
		}

		if (currentQs.equals(requestQs)) {
			if (requestFuture != null && requestFuture.isDone() && !getItems().isEmpty()) {
				// quick search already done and finished
				show();
			}
			return;
		}
		requestQs = currentQs;

		cancelQuickSearch();

		final String title = requestQs.toString();
		requestFuture = ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					requestQuickSearch(title);
				} catch (final InterruptedException e) {
					return;
				} catch (final Exception e) {
					LOGGER.error("loading {}", title, e);
					return;
				}
			}
		});
	}

	private void requestQuickSearch(final String title) throws InterruptedException, MalformedURLException, IOException {
		if (title.length() < 3) {
			// need more characters
			return;
		}

		Thread.sleep(500); // allow user to type more characters

		final String url = String.format(SEARCH_PATTERN, URLEncoder.encode(title, "UTF8"));
		final List<AllocineQsResult> qsResults = new ArrayList<AllocineQsResult>();
		LOGGER.info("requested {}", url);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
		final JsonArray jsonQsResults = new JsonParser().parse(reader).getAsJsonArray();
		for (final JsonElement jsonElement : jsonQsResults) {
			final AllocineQsResult qsResult = new AllocineQsResult(jsonElement.getAsJsonObject());
			if (!qsResult.getThumbnail().isEmpty()) {
				qsResults.add(qsResult);
			}
		}

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				LOGGER.info("displayed {}", url);
				getItems().clear();
				getItems().addAll(qsResults);
				if (!getItems().isEmpty()) {
					show();
				}
			}
		});
	}

	public void setSearchedText(final String searched) {
		final AllocineQsResult searchedQs = new AllocineQsResult(searched);
		if (!searchedQs.equals(this.searchedQs)) {
			this.searchedQs = searchedQs;
			setValue(searchedQs);
		}
	}

	public void setOnSearch(final Callback<AllocineQsResult, Void> callback) {
		searchCallback = callback;
	}
}
