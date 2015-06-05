package net.anfoya.java.net.download;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.java.net.filtered.FilteredHttpHandler;
import net.anfoya.java.net.filtered.engine.Matcher;

public class TorrentHttpHandler extends FilteredHttpHandler {
	public TorrentHttpHandler(final Matcher matcher) {
		super(matcher);
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		return url.toString().endsWith(".torrent")
			? new TorrentConnection(url)
			: super.openConnection(url);
	}
}
