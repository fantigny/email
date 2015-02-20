package net.anfoya.downloads.javafx.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class GoBackUrlConnection extends URLConnection {
	public GoBackUrlConnection(URL url) {
		super(url);
	}

	public GoBackUrlConnection() {
		this(null);
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream("<script>history.back()</script>".getBytes());
	}
}
