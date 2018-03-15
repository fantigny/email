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

	private final ReadOnlyBooleanWrapper isLatest;

	private final Timer timer;
	
	private String myVersion;
	private String latestVersion;

	public VersionHelper() {
		myVersion = "";
		latestVersion = "";
		isLatest = new ReadOnlyBooleanWrapper(true);
		timer = new Timer("version-checker", true);
	}
	
	public void start() {
		timer.scheduleAtFixedRate(new TimerTask() { @Override public void run() { refresh(); } }, 0, REFRESH_DELAY);
	}
	
	public void stop() {
		timer.cancel();
	}

	public String getMyVersion() {
		return myVersion;
	}

	public String getLatestVersion() {
		return latestVersion;
	}

	public ReadOnlyBooleanProperty isLastest() {
		return isLatest.getReadOnlyProperty();
	}

	public void refresh() {
		if (myVersion.isEmpty()) {
			try (final InputStream in = getClass().getResourceAsStream(VERSION_TXT_RESOURCE);
					final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				reader.lines().forEach(l -> myVersion = l);
			} catch (final IOException e) {
				LOGGER.error("error read version from {}", VERSION_TXT_RESOURCE, e);
			}
		}
		try (InputStream in = new URL(VERSION_TXT_URL).openConnection().getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			reader.lines().forEach(l -> latestVersion = l);
		} catch (final IOException e) {
			latestVersion = myVersion;
			LOGGER.error("error read version from {}", VERSION_TXT_URL, e);
		}
		isLatest.set(myVersion.equals(latestVersion));

		LOGGER.info("current ({}) == latest ({}) is {}", myVersion, latestVersion, isLatest.get());
	}
}
