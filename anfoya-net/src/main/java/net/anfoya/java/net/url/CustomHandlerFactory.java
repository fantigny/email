package net.anfoya.java.net.url;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.java.net.url.filter.Matcher;
import net.anfoya.java.net.url.handler.E2dLinkHandler;
import net.anfoya.java.net.url.handler.FilteredHandler;
import net.anfoya.java.net.url.handler.StartHandler;

public class CustomHandlerFactory implements URLStreamHandlerFactory {
	private final Matcher matcher;

	public CustomHandlerFactory(final Matcher matcher) {
		this.matcher = matcher;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("ed2k".equals(protocol)) {
			return new E2dLinkHandler();
		} else if ("magnet".equals(protocol)) {
			return new StartHandler();
		} else if ("http".equals(protocol)
				|| "https".equals(protocol)) {
			return new FilteredHandler(matcher);
		}
		return null;
	}
}
