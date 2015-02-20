package net.anfoya.movies.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MovieWebsite {

	public static final MovieWebsite[] LIST = {
			  new MovieWebsite("AlloCine"		, "www.allocine.fr"			, "/recherche/?q=%s"				, "fichefilm_gen_cfilm=")
			, new MovieWebsite("Rotten Tomatoes", "www.rottentomatoes.com"	, "/search/?search=%s"				,".com/m/")
			, new MovieWebsite("IMDb"			, "www.imdb.com"			, "/find?ref_=nv_sr_fn&q=%s&s=all"	,"/title/")
			, new MovieWebsite("Google"			, "www.google.com"			, "/search?q=%s"					, "")
			, new MovieWebsite("Internet"		, "https://www.google.com"	, ""								, "")
	};

	private final String name;
	private final String url;
	private final String searchPattern;
	private final String moviePagePattern;

	public MovieWebsite(final String name, String url, String searchPattern, final String moviePagePattern) {
		super();

		this.name = name;
		if (!url.contains("http")) {
			url = "http://" + url;
		}
		if (url.charAt(url.length() - 1) != '/') {
			url += "/";
		}
		this.url = url;
		if (!searchPattern.isEmpty() && searchPattern.charAt(0) == '/') {
			searchPattern = searchPattern.substring(1);
		}
		this.searchPattern = searchPattern;
		this.moviePagePattern = moviePagePattern;
	}

	public String getName() {
		return name;
	}

	public String getSearchUrl(final String movie) {
		String searchUrl;
		try {
			searchUrl = url + String.format(searchPattern, URLEncoder.encode(movie, "UTF-8"));
		} catch (final UnsupportedEncodingException e) {
			searchUrl = url + String.format(searchPattern, movie);
		}
		return searchUrl;
	}

	public String getUrl() {
		return url;
	}

	public Object getSearchPattern() {
		return searchPattern;
	}

	public String getMoviePagePattern() {
		return moviePagePattern;
	}

	public boolean isSearchable() {
		return !searchPattern.isEmpty();
	}

	public boolean hasMoviePagePattern() {
		return !moviePagePattern.isEmpty();
	}
}
