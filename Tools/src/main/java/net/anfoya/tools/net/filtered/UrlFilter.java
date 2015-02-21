package net.anfoya.tools.net.filtered;

import java.net.URL;

public interface UrlFilter {
	void loadFilters();
	boolean filtered(URL url);
}
