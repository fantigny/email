package net.anfoya.java.net.url.handler;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.java.net.url.connection.DownloadAndStartConnection;
import net.anfoya.java.net.url.connection.Ed2kFixConnection;
import net.anfoya.java.net.url.connection.EmptyUrlConnection;
import net.anfoya.java.net.url.filter.Matcher;
import sun.net.www.protocol.http.Handler;

public class FilteredHandler extends Handler {
	private final Matcher matcher;

	public FilteredHandler(final Matcher matcher) {
		this.matcher = matcher;
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		final String urlStr = url.toString();
		if (matcher != null && matcher.matches(urlStr)) {
			return new EmptyUrlConnection();
		} else if (urlStr.endsWith(".torrent")) {
			return new DownloadAndStartConnection(url);
		}
		return new Ed2kFixConnection(url);
	}
}
