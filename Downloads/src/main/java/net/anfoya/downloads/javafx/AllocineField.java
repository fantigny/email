package net.anfoya.downloads.javafx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AllocineField extends ComboBox<String> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineField.class);

	private class AllocineMovie {
		String title1;
		String title2;
		String thumbnail;
		public AllocineMovie(final JsonObject jsonMovie) {
			this.title1 = jsonMovie.get("title1").getAsString();
			this.title2 = jsonMovie.get("title2").getAsString();
			this.thumbnail = jsonMovie.get("thumbnail").getAsString();
			if (title1.isEmpty() && ! title2.isEmpty()) {
				title1 = title2;
				title2 = "";
			}
		}
	}

	public AllocineField() {
		setEditable(true);

		setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				updateList();
			}
		});
	}

	private void updateList() {
		getItems().clear();

		final String text = getValue();
		if (text.length() > 2) {
			try {
				final URL url = new URL("http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=" + text);
				final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
				final JsonArray jsonMovies = new JsonParser().parse(reader).getAsJsonArray();

				final List<AllocineMovie> movies = new ArrayList<AllocineMovie>();
				jsonMovies.forEach(new Consumer<JsonElement>() {
					@Override
					public void accept(final JsonElement element) {
						movies.add(new AllocineMovie(element.getAsJsonObject()));
					}
				});

				updateList(movies);
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void updateList(final List<AllocineMovie> movies) {
		for(final AllocineMovie movie: movies) {
			getItems().add(movie.title1);
		}
	}
}
