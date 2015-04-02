package net.anfoya.movie.connector;

import java.util.ArrayList;
import java.util.List;

import net.anfoya.movie.connector.MovieVo.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RottenTomatoesConnector extends QuickSearchMovieConnector implements MovieConnector {
	private static final String NAME = "Rotten Tomatoes";
	private static final String HOME_URL = "http://www.rottentomatoes.com";
	private static final String PATTERN_SEARCH = HOME_URL + "/search/?search=%s";
	private static final String PATTERN_QUICK_SEARCH = HOME_URL + "/search/json/?catCount=2&q=%s";
	private static final String PATTERN_MOVIE = HOME_URL + "/m/%s/?search=%s";

	public RottenTomatoesConnector() {
		super(NAME, HOME_URL, PATTERN_SEARCH, PATTERN_QUICK_SEARCH);
	}

	@Override
	protected List<MovieVo> buildVos(final JsonElement json) {
		final List<MovieVo> movieVos = new ArrayList<MovieVo>();

		final JsonArray jsonMovies = json.getAsJsonObject().get("movies").getAsJsonArray();
		for (final JsonElement jsonElement: jsonMovies) {
			final JsonObject jsonMovie = jsonElement.getAsJsonObject();
			movieVos.add(new MovieVo(
					getValue(jsonMovie, "vanity")
					, Type.MOVIE
					, getValue(jsonMovie, "name")
					, getValue(jsonMovie, "name")
					, getValue(jsonMovie, "year")
					, getValue(jsonMovie, "image", getClass().getResource("nothumbnail.png").toString())
					, String.format(PATTERN_MOVIE, getValue(jsonMovie, "vanity"), getValue(jsonMovie, "name"))
					, ""
					, ""
					, ""
					, ""
					, getName()));
		}

		return movieVos;
	}
}

