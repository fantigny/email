package net.anfoya.downloads.javafx;

import net.anfoya.easylist.net.filtered.EasyListFilterImpl;
import net.anfoya.tools.net.PersistentCookieStore;
import net.anfoya.tools.net.filtered.UrlFilter;

public class ComponentBuilder {
	private final PersistentCookieStore cookieStore;
	private final UrlFilter urlFilter;

	private final SearchTabs searchTabs;
	private final SearchPane searchPane;

	public ComponentBuilder() {
		cookieStore = new PersistentCookieStore();
		urlFilter = new EasyListFilterImpl();

		searchTabs = new SearchTabs();
		searchPane = new SearchPane();
	}

	public PersistentCookieStore buildCookieStore() {
		return cookieStore;
	}

	public SearchTabs buildSearchTabs() {
		return searchTabs;
	}

	public SearchPane buildSearchPane() {
		return searchPane;
	}

	public UrlFilter buildUrlFilter() {
		return urlFilter;
	}
}