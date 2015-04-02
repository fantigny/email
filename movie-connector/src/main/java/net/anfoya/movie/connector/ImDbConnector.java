package net.anfoya.movie.connector;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import net.anfoya.movie.connector.MovieVo.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ImDbConnector extends QuickSearchMovieConnector implements MovieConnector {
	private static final String NAME = "IMDb";
	private static final String HOME_URL = "http://www.imdb.com";
	private static final String PATTERN_SEARCH = HOME_URL + "/find?ref_=nv_sr_fn&q=%s&s=all";
	private static final String PATTERN_QUICK_SEARCH = "http://sg.media-imdb.com/suggests/%s/%s.json";
	private static final String PATTERN_MOVIE = HOME_URL + "/title/%s/";

	public ImDbConnector() {
		super(NAME, HOME_URL, PATTERN_SEARCH, PATTERN_QUICK_SEARCH, true);
	}

	@Override
	protected String getQuickSearchUrl(String pattern) throws UnsupportedEncodingException {
		pattern.replace(" ", "_");
		pattern = pattern.toLowerCase();
		pattern = URLEncoder.encode(pattern, "UTF8");
		return String.format(PATTERN_QUICK_SEARCH, pattern.charAt(0), pattern);
	}

	@Override
	protected List<MovieVo> buildVos(final JsonElement json) {
		final List<MovieVo> movieVos = new ArrayList<MovieVo>();

		final JsonArray jsonMovies = json.getAsJsonObject().get("d").getAsJsonArray();
		for (final JsonElement jsonElement: jsonMovies) {
			final JsonObject jsonMovie = jsonElement.getAsJsonObject();
			if (getValue(jsonMovie, "q", "").equals("feature")) {
				movieVos.add(new MovieVo(
						getValue(jsonMovie, "id")
						, Type.MOVIE
						, getValue(jsonMovie, "l")
						, getValue(jsonMovie, "l")
						, getValue(jsonMovie, "y")
						, jsonMovie.get("i").getAsJsonArray().get(0).toString()
						, String.format(PATTERN_MOVIE, getValue(jsonMovie, "id"))
						, ""
						, ""
						, ""
						, ""
						, getName()));
			}
		}

		return movieVos;
	}
}

