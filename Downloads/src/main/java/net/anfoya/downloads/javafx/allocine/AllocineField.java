package net.anfoya.downloads.javafx.allocine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class AllocineField extends ComboBox<AllocineMovie> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineField.class);

	private final AtomicLong requestId = new AtomicLong(0);
	private volatile String previousText;

	public AllocineField() {
		setEditable(true);
		setCellFactory(new Callback<ListView<AllocineMovie>, ListCell<AllocineMovie>>() {
			@Override
			public ListCell<AllocineMovie> call(final ListView<AllocineMovie> movie) {
				return new AllocineListCell();
			}
		});

		getEditor().setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				final String text = getEditor().getText();
				if (!text.equals(previousText)) {
					previousText = text;
					updateList(text);
				}
			}
		});
	}

	private void updateList(final String text) {
		final long requestId = this.requestId.incrementAndGet();
		if (getItems().size() > 0) {
			getItems().clear();
		}
		if (isShowing()) {
			hide();
		}
		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (final InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				updateList(text, requestId);
			}
		});
	}

	private void updateList(final String text, final long requestId) {

		if (this.requestId.get() != requestId) {
			return;
		}

		final List<AllocineMovie> movies = new ArrayList<AllocineMovie>();
		if (text.length() > 2) {
			try {
				final URL url = new URL("http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=" + URLEncoder.encode(text, "UTF8"));
				LOGGER.info("autocomplete allocine {}", url);
				final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (AllocineField.this.requestId.get() != requestId) {
					return;
				}
				getItems().addAll(movies);
				if (getItems().size() > 0) {
					show();
				}
			}
		});
	}

	public String getText() {
		return getEditor().getText();
	}

	public void setText(final String text) {
		if (!getEditor().getText().equals(text)) {
			getEditor().setText(text);
		}
	}

	public void setOnSearch(final EventHandler<ActionEvent> handler) {
		setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				if (!isShowing()) {
					handler.handle(event);
				}
			}
		});
		setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				if (event.getCode() == KeyCode.ENTER && isShowing()) {
					handler.handle(new ActionEvent(event.getSource(), event.getTarget()));
				}
			}
		});
	}
}
