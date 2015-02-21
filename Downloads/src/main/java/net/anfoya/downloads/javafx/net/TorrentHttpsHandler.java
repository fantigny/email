package net.anfoya.downloads.javafx.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.tools.net.filtered.FilteredHttpsHandler;
import net.anfoya.tools.net.filtered.UrlFilter;

public class TorrentHttpsHandler extends FilteredHttpsHandler {
	public TorrentHttpsHandler(final UrlFilter filter) {
		super(filter);
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		final URLConnection urlConnection = super.openConnection(url);
		return url.toString().endsWith(".torrent")
			? new TorrentConnection(url)
			: urlConnection;
	}
}
