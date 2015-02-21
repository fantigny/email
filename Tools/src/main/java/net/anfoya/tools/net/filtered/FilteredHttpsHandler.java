package net.anfoya.tools.net.filtered;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.tools.net.EmptyUrlConnection;
import sun.net.www.protocol.https.Handler;

public class FilteredHttpsHandler extends Handler {
	private final UrlFilter filter;

	public FilteredHttpsHandler(final UrlFilter filter) {
		this.filter = filter;
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		return filter.filtered(url)
				? new EmptyUrlConnection()
				: super.openConnection(url);
	}
}
