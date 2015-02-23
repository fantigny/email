package net.anfoya.tools.net.torrent;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.tools.net.filtered.FilteredHttpHandler;
import net.anfoya.tools.net.filtered.engine.RuleSet;

public class TorrentHttpHandler extends FilteredHttpHandler {
	public TorrentHttpHandler(final RuleSet filter) {
		super(filter);
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		return url.toString().endsWith(".torrent")
			? new TorrentConnection(url)
			: super.openConnection(url);
	}
}
