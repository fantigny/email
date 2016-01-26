package net.anfoya.mail.browser.javafx.css;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.Scene;

public class CssHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(CssHelper.class);

	private static final String CSS_EXT_PATH = "./css/";

	private static final String[] COMMON_CSS = {
			"/net/anfoya/mail/css/style.css"
			, "/net/anfoya/mail/css/dnd.css"
			, "/net/anfoya/mail/css/scrollbar.css"
	};

	public static void addCommonCss(Scene scene) {
		for(final String css: COMMON_CSS) {
			addCss(scene, css);
		}
	}

	public static void addCss(Scene scene, String internalPath) {
		final String externalPath = CSS_EXT_PATH + new File(internalPath).getName();
		final File file = new File(externalPath);
		String path;
		try {
			if (!file.exists()) {
				throw new FileNotFoundException();
			}
			path = file.toURI().toString();
		} catch (final IOException e) {
			path = CssHelper.class.getClass().getResource(internalPath).toExternalForm();
		}
		LOGGER.info("add css {}", path);
		scene.getStylesheets().add(path);
	}
}
