package net.anfoya.java.net.filtered;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.java.net.filtered.engine.Matcher;
import net.anfoya.java.net.filtered.engine.RuleSet;

public class FilteredHandlerFactory implements URLStreamHandlerFactory {
	private final Matcher matcher;

	public FilteredHandlerFactory(final RuleSet ruleSet) {
		this.matcher = new Matcher(ruleSet);
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("http".equals(protocol)) {
			return new FilteredHttpHandler(matcher);
		} else if ("https".equals(protocol)) {
			return new FilteredHttpsHandler(matcher);
		} else {
			return null;
		}
	}
}
