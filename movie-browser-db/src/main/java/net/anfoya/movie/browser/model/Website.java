package net.anfoya.movie.browser.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Website {

	private final String name;
	private final String host;
	private final String searchPattern;
	private final String defaultPattern;
	private final boolean freeInput;

	public Website(final String name, final String url, final String searchPattern, final String defaultPattern) {
		this(name, url, searchPattern, defaultPattern, false);
	}

	public Website(final String name, String host, String searchPattern, final String defaultPattern, final boolean freeInput) {
		super();

		this.name = name;
		if (!host.contains("http")) {
			host = "http://" + host;
		}
		if (host.charAt(host.length() - 1) != '/') {
			host += "/";
		}
		this.host = host;
		if (!searchPattern.isEmpty() && searchPattern.charAt(0) == '/') {
			searchPattern = searchPattern.substring(1);
		}
		this.searchPattern = searchPattern;
		this.defaultPattern = defaultPattern;
		this.freeInput = freeInput;
	}

	public String getName() {
		return name;
	}

	public String getSearchUrl(final String search) {
		String searchUrl;
		try {
			searchUrl = host + String.format(searchPattern, URLEncoder.encode(search, "UTF-8"));
		} catch (final UnsupportedEncodingException e) {
			searchUrl = host + String.format(searchPattern, search);
		}
		return searchUrl;
	}

	public String getHomeUrl() {
		return host;
	}

	public Object getSearchPattern() {
		return searchPattern;
	}

	public String getDefaultPattern() {
		return defaultPattern;
	}

	public boolean isFreeInput() {
		return freeInput;
	}

	public boolean isSearchable() {
		return !searchPattern.isEmpty();
	}

	public boolean hasDefaultPattern() {
		return !defaultPattern.isEmpty();
	}

	@Override
	public String toString() {
		return getName();
	}
}
