package net.anfoya.mail.browser.javafx.util;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;

public class UrlHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(UrlHelper.class);

	public static void open(String url, Callback<String, Void> onMailto) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (final URISyntaxException e) {
			LOGGER.error("reading address {}", url, e);
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
					LOGGER.error("handling link {}", url, e);
				}
			}, "handling url " + url);
		}
	}
}
