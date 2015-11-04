package net.anfoya.mail.browser.javafx.settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionChecker {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionChecker.class);
	private static final String VERSION_FILEPATH = "/version.txt";
	private static final String VERSION_URL = "http://81.108.162.255/version.txt";

	private static String version;

	public String getVersion() {
		if (version != null) {
			return version;
		}

		final StringBuilder sb = new StringBuilder();
		final InputStream in = getClass().getResourceAsStream(VERSION_FILEPATH);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (final IOException e) {
			LOGGER.error("error reading version from {}", VERSION_FILEPATH, e);
		}
		if (sb.length() != 0) {
			version = sb.toString();
		}

		return version;
	}

	public String getLastestVesion() {
		final StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new URL(VERSION_URL).openStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (final IOException e) {
			LOGGER.error("error reading version from {}", VERSION_URL, e);
		}

		return sb.toString();
	}

	public boolean isLastVersion() {
		return getVersion().equals(getLastestVesion());
	}
}
