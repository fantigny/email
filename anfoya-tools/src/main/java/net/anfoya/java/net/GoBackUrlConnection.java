package net.anfoya.java.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class GoBackUrlConnection extends URLConnection {
	private static final String GO_BACK_HTML_TEMPLATE = ""
		+ "<html><body onLoad='history.back()'>"
			+ "<br>"
			+ "&nbsp;&nbsp;&nbsp;&nbsp;"
			+ "<font face='verdana'><i>"
					+ "%s"
			+ "</i></font>"
		+ "</body></html>";

	private final byte[] goBackHtml;

	public GoBackUrlConnection(final String msg) {
		super(null);
		goBackHtml = String.format(GO_BACK_HTML_TEMPLATE, msg == null? "": msg).getBytes();
	}

	public GoBackUrlConnection() {
		this(null);
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(goBackHtml);
	}
}
