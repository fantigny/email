package net.anfoya.downloads.javafx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.function.Consumer;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class AllocineFieldTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineFieldTest.class);

	@Test
	public void updateList() {
		updateList("Lion");
	}

	private void updateList(final String text) {
		try {
			final URL url = new URL("http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=" + text);
			final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			final JsonArray movies = new JsonParser().parse(reader).getAsJsonArray();
			movies.forEach(new Consumer<JsonElement>() {
				@Override
				public void accept(final JsonElement element) {
					final JsonObject movie = element.getAsJsonObject();
					LOGGER.info("allocine: {} {}", movie.get("title1"), movie.get("title2"));
				}
			});
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
