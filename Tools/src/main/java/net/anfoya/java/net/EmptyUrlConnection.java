package net.anfoya.java.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class EmptyUrlConnection extends URLConnection {
	public EmptyUrlConnection() {
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
