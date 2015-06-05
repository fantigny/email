package net.anfoya.java.net.download;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.java.net.filtered.engine.Matcher;
import net.anfoya.java.net.filtered.engine.RuleSet;

public class DownloadHandlerFactory implements URLStreamHandlerFactory {
	private final Matcher matcher;

	public DownloadHandlerFactory(final RuleSet ruleSet) {
		this.matcher = new Matcher(ruleSet);
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("magnet".equals(protocol)) {
			return new DownloadLinkHandler();
		} else if ("ed2k".equals(protocol)) {
			return new DownloadLinkHandler();
		} else if ("http".equals(protocol)) {
			return new TorrentHttpHandler(matcher);
		} else if ("https".equals(protocol)) {
			return new TorrentHttpsHandler(matcher);
		} else {
			return null;
		}
	}
}
