package net.anfoya.java.net.url.handler;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class E2dLinkHandler extends StartHandler {

	@Override
	protected URLConnection openConnection(final URL u) throws IOException {
		return super.openConnection(u.toString().replaceAll("-", "|"));
	}
}
