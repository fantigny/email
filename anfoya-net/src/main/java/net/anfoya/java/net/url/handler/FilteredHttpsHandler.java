package net.anfoya.java.net.url.handler;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import net.anfoya.java.net.url.connection.DownloadAndStartConnection;
import net.anfoya.java.net.url.connection.Ed2kWorkAroundHttpsConnection;
import net.anfoya.java.net.url.connection.EmptyConnection;
import net.anfoya.java.net.url.filter.Matcher;
import sun.net.www.protocol.https.Handler;

public class FilteredHttpsHandler extends Handler {
	private final Matcher matcher;

	public FilteredHttpsHandler(final Matcher matcher) {
		this.matcher = matcher;
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		final String urlStr = url.toString();
		if (urlStr.endsWith(".torrent")) {
			return new DownloadAndStartConnection(url);
		} else if (matcher != null && matcher.matches(urlStr)) {
			return new EmptyConnection();
		} else {
			return new Ed2kWorkAroundHttpsConnection((HttpsURLConnection) super.openConnection(url));
		}
	}
}
