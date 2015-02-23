package net.anfoya.java.net.torrent;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.java.net.filtered.FilteredHttpsHandler;
import net.anfoya.java.net.filtered.engine.RuleSet;

public class TorrentHttpsHandler extends FilteredHttpsHandler {
	public TorrentHttpsHandler(final RuleSet filter) {
		super(filter);
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		return url.toString().endsWith(".torrent")
			? new TorrentConnection(url)
			: super.openConnection(url);
	}
}
