package net.anfoya.mail.browser.javafx.settings;

import static net.anfoya.mail.browser.javafx.settings.Settings.VERSION_TXT_RESOURCE;
import static net.anfoya.mail.browser.javafx.settings.Settings.VERSION_TXT_URL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

public class VersionHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionHelper.class);
	private static final long REFRESH_DELAY = 1000 * 60 * 60 * 6;

	private final ReadOnlyBooleanWrapper isLatestProperty;

	private String myVersion;
	private String latestVersion;

	public VersionHelper() {
		myVersion = "";
		latestVersion = "";
		isLatestProperty = new ReadOnlyBooleanWrapper(true);

		new Timer("version-checker", true).scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				refresh();
				LOGGER.info("current ({}) == latest ({}) is {}"
						, myVersion
						, latestVersion
						, isLatestProperty.get());
			}
		}, 0, REFRESH_DELAY);
	}

	public String getMyVersion() {
		return myVersion;
	}

	public String getLatestVersion() {
		return latestVersion;
	}

	public ReadOnlyBooleanProperty isLastestProperty() {
		return isLatestProperty.getReadOnlyProperty();
	}

	public void refresh() {
		if (myVersion.isEmpty()) {
			try (final InputStream in = getClass().getResourceAsStream(VERSION_TXT_RESOURCE);
					final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				reader.lines().forEach(l -> myVersion = l);
			} catch (final IOException e) {
				LOGGER.error("error reading version from {}", VERSION_TXT_RESOURCE, e);
			}
		}
		try (InputStream in = new URL(VERSION_TXT_URL).openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			reader.lines().forEach(l -> latestVersion = l);
		} catch (final IOException e) {
			LOGGER.error("error reading version from {}", VERSION_TXT_URL, e);
		}
		isLatestProperty.set(myVersion.equals(latestVersion));
	}
}
