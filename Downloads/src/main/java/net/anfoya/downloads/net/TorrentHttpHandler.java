package net.anfoya.downloads.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.tools.net.FilteredHttpHandler;
import net.anfoya.tools.net.UrlFilter;

public class TorrentHttpHandler extends FilteredHttpHandler {
	private TorrentHandler delegate;

	public TorrentHttpHandler(final UrlFilter filter) {
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
