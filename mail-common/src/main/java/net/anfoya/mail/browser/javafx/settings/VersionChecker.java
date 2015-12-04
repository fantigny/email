package net.anfoya.mail.browser.javafx.settings;

import static net.anfoya.mail.browser.javafx.settings.Settings.VERSION_FILEPATH;
import static net.anfoya.mail.browser.javafx.settings.Settings.VERSION_URL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionChecker {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionChecker.class);

	private static String version;
	private static String latest;

	public String getVersion() {
		if (version == null) {
			final StringBuilder sb = new StringBuilder();
			try (InputStream in = getClass().getResourceAsStream(VERSION_FILEPATH);
					BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				reader.lines().forEach(l -> sb.append(l));
			} catch (final IOException e) {
				LOGGER.error("error reading version from {}", VERSION_FILEPATH, e);
			}
			if (sb.length() > 0) {
				version = sb.toString();
			}
		}

		return version;
	}

	public String getLastestVesion() {
		if (latest == null) {
			final StringBuilder sb = new StringBuilder();
			try (InputStream in = new URL(VERSION_URL).openStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				reader.lines().forEach(l -> sb.append(l));
			} catch (final IOException e) {
				LOGGER.error("error reading version from {}", VERSION_URL, e);
			}
			if (sb.length() > 0) {
				latest = sb.toString();
			}
		}

		return latest;
	}

	public boolean isLastVersion() {
		LOGGER.debug("current {}, latest {}", getVersion(), getLastestVesion());
		return getVersion().equals(getLastestVesion());
	}

	public boolean isDisconnected() {
		return getVersion() == null;
	}
}
