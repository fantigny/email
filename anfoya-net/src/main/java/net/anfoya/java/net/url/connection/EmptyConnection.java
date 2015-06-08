package net.anfoya.java.net.url.connection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class EmptyConnection extends URLConnection {
	public EmptyConnection() {
		super(null);
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(new byte[0]);
	}
}
