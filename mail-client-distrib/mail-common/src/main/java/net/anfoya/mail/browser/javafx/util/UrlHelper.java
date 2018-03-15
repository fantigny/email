package net.anfoya.mail.browser.javafx.util;

import java.awt.Desktop;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.util.VoidCallback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;

public class UrlHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(UrlHelper.class);

	public static void open(String url) {
		open(url, null);
	}

	public static void open(String url, VoidCallback<String> composeCallback) {
		final URI uri = URI.create(url);
		if (uri.getScheme().equals("mailto")) {
			composeCallback.call(uri.getSchemeSpecificPart());
		} else {
			ThreadPool.getDefault().submit(PoolPriority.MAX, "open " + url, () -> {
				try {
					Desktop.getDesktop().browse(uri);
				} catch (final Exception e) {
					LOGGER.error("open {}", url, e);
				}
			});
		}
	}
}
