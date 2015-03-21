package net.anfoya.downloads.javafx.allocine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class AllocineQuickSearch implements QuickSearchProvider, DirectAccessProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineQuickSearch.class);
	private static final String SEARCH_PATTERN = "http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=%s";

	@Override
	public List<QuickSearchVo> search(final String pattern) {
		LOGGER.debug("search \"{}\"", pattern);
		final List<QuickSearchVo> qsResults = new ArrayList<QuickSearchVo>();

		// get a connection
		String url;
		try {
			url = String.format(SEARCH_PATTERN, URLEncoder.encode(pattern, "UTF8"));
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
			qsResults.add(new QuickSearchVo(jsonElement.getAsJsonObject()));
		}

		return qsResults;
	}

	@Override
	public URL getDirectUrl(final QuickSearchVo resultVo) {
		String pattern;
		if (resultVo.isPerson()) {
			pattern = "http://www.allocine.fr/personne/fichepersonne_gen_cpersonne=%s.html";
		} else if (resultVo.isSerie()) {
			pattern = "http://www.allocine.fr/series/ficheserie_gen_cserie=%s.html";
		} else {
			pattern = "http://www.allocine.fr/film/fichefilm_gen_cfilm=%s.html";
		}
		try {
			return new URL(String.format(pattern, resultVo.getId()));
		} catch (final MalformedURLException e) {
			LOGGER.error("creating url from ({})", resultVo);
			return null;
		}
	}

}
