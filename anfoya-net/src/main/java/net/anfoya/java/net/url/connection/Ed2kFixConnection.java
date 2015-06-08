package net.anfoya.java.net.url.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.java.io.Ed2kFilterInputStream;
import sun.net.www.protocol.http.Handler;

public class Ed2kFixConnection extends URLConnection {
	public Ed2kFixConnection(final URL url) {
		super(url);
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() throws MalformedURLException, IOException {
		return new Ed2kFilterInputStream(new URL(null, url.toString(), new Handler()).openStream()); // avoid handler factory re-entrance
	}
}
