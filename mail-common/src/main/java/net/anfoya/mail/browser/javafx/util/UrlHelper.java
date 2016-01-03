package net.anfoya.mail.browser.javafx.util;

import java.awt.Desktop;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;

public class UrlHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(UrlHelper.class);

	public static void open(String url) {
		open(url, null);
	}

	public static void open(String url, Callback<String, Void> onMailto) {
		final URI uri = URI.create(url);
		if (uri.getScheme().equals("mailto")) {
			onMailto.call(uri.getSchemeSpecificPart());
		} else {
			ThreadPool.getThreadPool().submitHigh(() -> {
				try {
					Desktop.getDesktop().browse(uri);
				} catch (final Exception e) {
					LOGGER.error("open {}", url, e);
				}
			}, "open " + url);
		}
	}
}
