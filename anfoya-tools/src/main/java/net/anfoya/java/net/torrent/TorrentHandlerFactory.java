package net.anfoya.java.net.torrent;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.java.net.filtered.engine.Matcher;
import net.anfoya.java.net.filtered.engine.RuleSet;

public class TorrentHandlerFactory implements URLStreamHandlerFactory {
	private final Matcher matcher;

	public TorrentHandlerFactory(final RuleSet ruleSet) {
		this.matcher = new Matcher(ruleSet);
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("magnet".equals(protocol)) {
			return new MagnetHandler();
		} else if ("http".equals(protocol)) {
			return new TorrentHttpHandler(matcher);
		} else if ("https".equals(protocol)) {
			return new TorrentHttpsHandler(matcher);
		} else {
			return null;
		}
	}
}
