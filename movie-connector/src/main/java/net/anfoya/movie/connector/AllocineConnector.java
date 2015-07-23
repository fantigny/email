package net.anfoya.movie.connector;

import java.util.ArrayList;
import java.util.List;

import net.anfoya.movie.connector.MovieVo.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AllocineConnector extends SuggestedMovieConnector implements MovieConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineConnector.class);

	private static final String NAME = "AlloCine";
	private static final String HOME_URL = "http://www.allocine.fr";
	private static final String PATTERN_SEARCH = HOME_URL + "/recherche/?q=%s";
	private static final String PATTERN_QUICK_SEARCH = "http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=%s";

	private static final String PATTERN_PERSON = HOME_URL + "/personne/fichepersonne_gen_cpersonne=%s.html";
	private static final String PATTERN_SERIE = HOME_URL + "/series/ficheserie_gen_cserie=%s.html";
	private static final String PATTERN_MOVIE = HOME_URL + "/film/fichefilm_gen_cfilm=%s.html";

	public AllocineConnector() {
		super(NAME, HOME_URL, PATTERN_SEARCH, PATTERN_QUICK_SEARCH);
	}

	@Override
	protected List<MovieVo> buildVos(final JsonElement json) {
		final List<MovieVo> movieVos = new ArrayList<MovieVo>();
		for (final JsonElement jsonElement: json.getAsJsonArray()) {
			movieVos.add(buildVo(jsonElement.getAsJsonObject()));
		}

		return movieVos;
	}

	private MovieVo buildVo(final JsonObject json) {
		final String id = getValue(json, "id");
		final String director = getMetadata(json, "director", "n/d");
		final String activity = getMetadata(json, "activity", "n/d");
		final String creator = getMetadata(json, "creator", "n/d");

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

		final String thumbnail = getValue(json, "thumbnail", getDefaultThumbnail());

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

		return new MovieVo(id, type, name, french, year, thumbnail, url, director, activity, creator, country, getName());
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
