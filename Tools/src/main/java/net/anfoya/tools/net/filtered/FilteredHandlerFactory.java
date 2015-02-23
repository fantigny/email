package net.anfoya.tools.net.filtered;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.tools.net.filtered.engine.RuleSet;

public class FilteredHandlerFactory implements URLStreamHandlerFactory {
	private final RuleSet filter;

	public FilteredHandlerFactory(final RuleSet filter) {
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
