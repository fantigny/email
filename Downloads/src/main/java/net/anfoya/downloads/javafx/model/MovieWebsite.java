package net.anfoya.downloads.javafx.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MovieWebsite {

	public static final MovieWebsite[] LIST = {
		  	new MovieWebsite("DVD Release", "www.dvdrip-fr.com", "/Site/dernieres_releases.php?type=letter", "/Site/recherche.php?recherche=%s", "")
		  	, new MovieWebsite("AlloCine", "www.allocine.fr", "", "/recherche/?q=%s", "fichefilm_gen_cfilm=")
			, new MovieWebsite("Rotten Tomatoes", "www.rottentomatoes.com", "", "/search/?search=%s",".com/m/")
			, new MovieWebsite("IMDb", "www.imdb.com", "", "/find?ref_=nv_sr_fn&q=%s&s=all","/title/")
			, new MovieWebsite("Pirate Bay", "https://pirateproxy.sx", "", "/search.php?q=%s", "")
			, new MovieWebsite("C Pas Bien", "www.cpasbien.pw", "", "/recherche/%s.html", "")
			, new MovieWebsite("Google", "www.google.com", "", "/search?q=%s", "")
	};

	private final String name;
	private final String address;
	private final String homePattern;
	private final String searchPattern;
	private final String moviePagePattern;

	private MovieWebsite(final String name, String address, String homePattern, String searchPattern, String moviePagePattern) {
		super();

		this.name = name;
		if (!address.contains("http")) {
			address = "http://" + address;
		}
		if (address.charAt(address.length() - 1) != '/') {
			address += "/";
		}
		this.address = address;
		if (!homePattern.isEmpty() && homePattern.charAt(0) == '/') {
			homePattern = homePattern.substring(1);
		}
		this.homePattern = homePattern;
		if (!searchPattern.isEmpty() && searchPattern.charAt(0) == '/') {
			searchPattern = searchPattern.substring(1);
		}
		this.searchPattern = searchPattern;
		if (!moviePagePattern.isEmpty() && moviePagePattern.charAt(0) == '/') {
			moviePagePattern = moviePagePattern.substring(1);
		}
		this.moviePagePattern = moviePagePattern;
	}

	public String getName() {
		return name;
	}

	public String getSearchUrl(final String movie) {
		String searchUrl;
		try {
			searchUrl = address + String.format(searchPattern, URLEncoder.encode(movie, "UTF-8"));
		} catch (final UnsupportedEncodingException e) {
			searchUrl = address + String.format(searchPattern, movie);
		}
		return searchUrl;
	}

	public String getHomeUrl() {
		return address + homePattern;
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
