package net.anfoya.tools.net;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.tools.net.filter.UrlFilter;

public class FilteredHandlerFactory implements URLStreamHandlerFactory {
	private final UrlFilter filter;

	public FilteredHandlerFactory(final UrlFilter filter) {
		this.filter = filter;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("http".equals(protocol)) {
			return new FilteredHttpHandler(filter);
		} else if ("https".equals(protocol)) {
			return new FilteredHttpsHandler(filter);
		} else {
			return null;
		}
	}
}
