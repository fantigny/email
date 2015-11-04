package net.anfoya.mail.browser.javafx.settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionChecker {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionChecker.class);

	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	public String getLastestVesion() {
		final StringBuilder sb = new StringBuilder();
		final String url = "";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (final IOException e) {
			LOGGER.error("error reading version from {}", url, e);
		}
		return sb.toString();
	}

	public boolean isLastVersion() {
		return true;
	}
}
