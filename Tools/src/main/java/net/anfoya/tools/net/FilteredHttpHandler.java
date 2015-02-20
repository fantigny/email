package net.anfoya.tools.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import sun.net.www.protocol.http.Handler;

public class FilteredHttpHandler extends Handler {
	private final UrlFilter filter;

	public FilteredHttpHandler(final UrlFilter filter) {
		this.filter = filter;
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		URLConnection urlConnection;
		if (filter.filtered(url)) {
			throw new UnsupportedOperationException("filtered by easylist");
		} else {
			urlConnection = super.openConnection(url);
		}

		return urlConnection;
	}
}
