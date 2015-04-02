package net.anfoya.movie.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import net.anfoya.movie.connector.MovieVo.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class SuggestedMovieConnector extends SimpleMovieConnector implements MovieConnector {
	private static final Logger LOGGER = LoggerFactory.getLogger(SuggestedMovieConnector.class);

	private static String defaultThumbnail;

	private final String quickSearchPattern;
	private final boolean jsonP;

	public SuggestedMovieConnector(final String name, final String homeUrl, final String searchPattern, final String quickSearchPattern) {
		this(name, homeUrl, searchPattern, quickSearchPattern, false);
	}

	public SuggestedMovieConnector(final String name, final String homeUrl, final String searchPattern, final String quickSearchPattern, final boolean jsonP) {
		super(name, homeUrl, searchPattern);
		this.quickSearchPattern = quickSearchPattern;
		this.jsonP = jsonP;
	}

	protected abstract List<MovieVo> buildVos(final JsonElement json);

	@Override
	public List<MovieVo> suggest(String pattern) {
		pattern = normalisePattern(pattern);
		LOGGER.debug("quick search \"{}\"", pattern);
		final List<MovieVo> qsResults = new ArrayList<MovieVo>();

		// get a connection
		String url;
		try {
			url = getQuickSearchUrl(pattern);
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
			LOGGER.error("reading from ({})", url, e);
			return qsResults;
		}

		// read / parse json data
		final JsonElement json;
		if (jsonP) {
			json = getSuggestedFromJsonP(reader);
		} else {
			json = new JsonParser().parse(reader);
		}

		// build result
		return buildVos(json);
	}

	@Override
	public MovieVo find(String pattern) {
		MovieVo bestMatch = null;
		final List<MovieVo> movieVos = suggest(pattern);
		if (!movieVos.isEmpty()) {
			pattern = normalisePattern(pattern);
			for(final MovieVo movieVo: movieVos) {
				final String name = normalisePattern(movieVo.getName());
				final String french = normalisePattern(movieVo.getFrench());
				if (name.startsWith(pattern) || french.startsWith(pattern)) {
					if (bestMatch == null) {
						bestMatch = movieVo;
					} else {
						if (bestMatch.getYear().compareTo(movieVo.getYear()) < 0) {
							bestMatch = movieVo;
						}
					}
				}
			}
		}
		if (bestMatch == null) {
			bestMatch = new MovieVo("", Type.UNDEFINED, pattern, "", "", "", getSearchUrl(pattern), "", "", "", "", getName());
		}

		return bestMatch;
	}

	protected String getDefaultThumbnail() {
		if (defaultThumbnail == null) {
			defaultThumbnail = getClass().getResource("Thumbnail.png").toString();
		}
		return defaultThumbnail;
	}

	protected String normalisePattern(final String text) {
		return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^\\p{ASCII}]", "").toLowerCase();
	}

	protected String getQuickSearchUrl(final String pattern) throws UnsupportedEncodingException {
		return String.format(quickSearchPattern, URLEncoder.encode(pattern, "UTF8"));
	}

	protected String getValue(final JsonObject jsonObject, final String id, final String... defaultVal) {
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

	private JsonElement getSuggestedFromJsonP(final BufferedReader reader) {
		final StringBuilder jsonp = new StringBuilder();
		String line;
		try {
			while((line=reader.readLine()) != null) {
				jsonp.append(line);
			}
		} catch (final IOException e) {
			return null;
		}

		final String json = jsonp.substring(jsonp.indexOf("(") + 1, jsonp.lastIndexOf(")"));
		return new JsonParser().parse(json);
	}
}
