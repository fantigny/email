package net.anfoya.java.net.url.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import net.anfoya.java.io.Ed2kFilterInputStream;

public class Ed2kFixConnection extends URLConnection {
	public Ed2kFixConnection(final URL url) {
		super(url);
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() throws MalformedURLException, IOException {
		URLStreamHandler handler;
		if ("http".equals(url.getProtocol())) {
			handler = new sun.net.www.protocol.http.Handler();
		} else if ("https".equals(url.getProtocol())) {
			handler = new sun.net.www.protocol.https.Handler();
		} else {
			handler = null;
		}
		return handler == null? null: new Ed2kFilterInputStream(new URL(null, url.toString(), handler).openStream()); // avoid handler factory re-entrance
	}
}
