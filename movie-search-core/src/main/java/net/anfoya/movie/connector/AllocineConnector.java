package net.anfoya.movie.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import net.anfoya.movie.connector.QuickSearchVo.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AllocineConnector extends AbstractConnector implements MovieConnector {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineConnector.class);
	private static final String PATTERN_SEARCH = "http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=%s";
	private static final String PATTERN_PERSON = "http://www.allocine.fr/personne/fichepersonne_gen_cpersonne=%s.html";
	private static final String PATTERN_SERIE = "http://www.allocine.fr/series/ficheserie_gen_cserie=%s.html";
	private static final String PATTERN_MOVIE = "http://www.allocine.fr/film/fichefilm_gen_cfilm=%s.html";

	@Override
	public List<QuickSearchVo> find(final String pattern) {
		LOGGER.debug("search \"{}\"", pattern);
		final List<QuickSearchVo> qsResults = new ArrayList<QuickSearchVo>();

		// get a connection
		String url;
		try {
			url = String.format(PATTERN_SEARCH, URLEncoder.encode(pattern, "UTF8"));
		} catch (final UnsupportedEncodingException e) {
			LOGGER.error("encoding \"{}\"", pattern);
			return qsResults;
		}
		LOGGER.info("request \"{}\"", url);
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
		} catch (final MalformedURLException e) {
			LOGGER.error("invalid URL ({})", url);
			return qsResults;
		} catch (final IOException e) {
			LOGGER.error("reading from ({})", url);
			return qsResults;
		}

		// read / parse json data
		final JsonArray jsonQsResults = new JsonParser().parse(reader).getAsJsonArray();
		for (final JsonElement jsonElement : jsonQsResults) {
			qsResults.add(buildVo(jsonElement.getAsJsonObject()));
		}

		return qsResults;
	}

	private QuickSearchVo buildVo(final JsonObject json) {
		final String id = getValue(json, "id");
		final String director = getMetadata(json, "director");
		final String activity = getMetadata(json, "activity");
		final String creator = getMetadata(json, "creator");

		final Type type;
		final String entityType = getValue(json, "entitytype");
		if (entityType.equals("person")) {
			type = Type.PERSON;
		} else if (entityType.equals("series")) {
			type = Type.SERIE;
		} else {
			type = Type.UNDEFINED;
		}

		final String name;
		if (type == Type.PERSON) {
			name = getValue(json, "title1");
		} else {
			name = getValue(json, "title2");
		}

		String year;
		if (type == Type.SERIE) {
			year = getMetadata(json, "yearstart");
		} else {
			year = getMetadata(json, "productionyear", "");
		}

		final String french = getValue(json, "title1", name);

		final String thumbnail = getValue(json, "thumbnail", getClass().getResource("nothumbnail.png").toString());

		final String country = getMetadata(json, "nationality", "");

		final String url;
		switch (type) {
		case PERSON:
			url = String.format(PATTERN_PERSON, id);
			break;
		case SERIE:
			url = String.format(PATTERN_SERIE, id);
			break;
		default:
			url = String.format(PATTERN_MOVIE, id);
			break;
		}

		return new QuickSearchVo(id, type, name, french, year, thumbnail, url, director, activity, creator, country);
	}

	private String getValue(final JsonObject jsonObject, final String id, final String... defaultVal) {
		String value;
		try {
			value = jsonObject.get(id).getAsString();
		} catch (final Exception e) {
			if (defaultVal.length != 0) {
				value = defaultVal[0];
			} else {
				value = "";
				LOGGER.warn("{} not found in {}", id, jsonObject.toString(), e);
			}
		}
		return value;
	}

	private String getMetadata(final JsonObject jsonObject, final String id, final String... defaultVal) {
		String metadata = "";
		try {
			final JsonArray jsonArray = jsonObject.get("metadata").getAsJsonArray();
			for (final JsonElement jsonElement : jsonArray) {
				final JsonObject jsonMetadata = jsonElement.getAsJsonObject();
				if (jsonMetadata.has("property") && jsonMetadata.get("property").getAsString().equals(id)) {
					if (!metadata.isEmpty()) {
						metadata += ", ";
					}
					metadata += jsonMetadata.get("value").getAsString();
				}
			}
		} catch (final Exception e) {
			if (defaultVal.length != 0) {
				metadata = defaultVal[0];
			} else {
				metadata = "";
				LOGGER.warn("{} not found in {}", id, jsonObject.toString(), e);
			}
		}
		return metadata;
	}
}
