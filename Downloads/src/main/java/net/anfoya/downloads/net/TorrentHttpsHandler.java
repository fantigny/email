package net.anfoya.downloads.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.tools.net.FilteredHttpsHandler;
import net.anfoya.tools.net.UrlFilter;

public class TorrentHttpsHandler extends FilteredHttpsHandler {
	private final TorrentHandler delegate;

	public TorrentHttpsHandler(final UrlFilter filter) {
		super(filter);
		delegate = new TorrentHandler();
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		final URLConnection urlConnection = super.openConnection(url);
		if (urlConnection != null && url.toString().endsWith(".torrent")) {
			delegate.launch(url);
			throw new UnsupportedOperationException("filtered torrent");
		}

		return urlConnection;
	}
}
