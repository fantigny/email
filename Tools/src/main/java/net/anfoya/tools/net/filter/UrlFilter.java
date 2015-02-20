package net.anfoya.tools.net.filter;

import java.net.URL;

public interface UrlFilter {
	void loadFilters();
	boolean filtered(URL url);
}
