package net.anfoya.tools.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Website {

	private final String name;
	private final String host;
	private final String homePattern;
	private final String searchPattern;
	private final String defaultPattern;

	public Website(final String name, final String url, final String searchPattern, final String defaultPattern) {
		this(name, url, "", searchPattern, defaultPattern);
	}

	public Website(final String name, String host, String homePattern, String searchPattern, final String defaultPattern) {
		super();

		this.name = name;
		if (!host.contains("http")) {
			host = "http://" + host;
		}
		if (host.charAt(host.length() - 1) != '/') {
			host += "/";
		}
		this.host = host;
		if (!homePattern.isEmpty() && homePattern.charAt(0) == '/') {
			homePattern = homePattern.substring(1);
		}
		this.homePattern = homePattern;
		if (!searchPattern.isEmpty() && searchPattern.charAt(0) == '/') {
			searchPattern = searchPattern.substring(1);
		}
		this.searchPattern = searchPattern;
		this.defaultPattern = defaultPattern;
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
		return host + (homePattern.isEmpty()? "" : homePattern);
	}

	public Object getSearchPattern() {
		return searchPattern;
	}

	public String getDefaultPattern() {
		return defaultPattern;
	}

	public boolean isSearchable() {
		return !searchPattern.isEmpty();
	}

	public boolean hasDefaultPattern() {
		return !defaultPattern.isEmpty();
	}

	public String getHomePattern() {
		return homePattern;
	}
}
