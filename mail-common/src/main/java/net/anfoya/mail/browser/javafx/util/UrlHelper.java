package net.anfoya.mail.browser.javafx.util;

import java.awt.Desktop;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;

public class UrlHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(UrlHelper.class);

	public static void open(String url) {
		open(url, null);
	}

	public static void open(String urlStr, Callback<String, Void> onMailto) {
		URI uri;
		try {
			final URL url = new URL(urlStr);
			uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), null);
		} catch (final URISyntaxException | MalformedURLException e) {
			LOGGER.error("reading address {}", urlStr, e);
			return;
		}
		final String scheme = uri.getScheme();
		if (scheme.equals("mailto")) {
			onMailto.call(uri.getSchemeSpecificPart());
		} else {
			ThreadPool.getInstance().submitHigh(() -> {
				try {
					Desktop.getDesktop().browse(uri);
				} catch (final Exception e) {
					LOGGER.error("handling link {}", urlStr, e);
				}
			}, "handling url " + urlStr);
		}
	}
}
