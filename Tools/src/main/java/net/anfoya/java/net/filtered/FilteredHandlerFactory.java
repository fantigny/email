package net.anfoya.java.net.filtered;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.java.net.filtered.engine.RuleSet;

public class FilteredHandlerFactory implements URLStreamHandlerFactory {
	private final RuleSet ruleSet;

	public FilteredHandlerFactory(final RuleSet ruleSet) {
		this.ruleSet = ruleSet;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("http".equals(protocol)) {
			return new FilteredHttpHandler(ruleSet);
		} else if ("https".equals(protocol)) {
			return new FilteredHttpsHandler(ruleSet);
		} else {
			return null;
		}
	}
}
