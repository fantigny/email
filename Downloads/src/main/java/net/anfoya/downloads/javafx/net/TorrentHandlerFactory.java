package net.anfoya.downloads.javafx.net;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.tools.net.filtered.UrlFilter;

public class TorrentHandlerFactory implements URLStreamHandlerFactory {
	private final UrlFilter filter;

	public TorrentHandlerFactory(final UrlFilter urlFilter) {
		this.filter = urlFilter;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("magnet".equals(protocol)) {
			return new MagnetHandler();
		} else if ("http".equals(protocol)) {
			return new TorrentHttpHandler(filter);
		} else if ("https".equals(protocol)) {
			return new TorrentHttpsHandler(filter);
		} else {
			return null;
		}
	}
}
