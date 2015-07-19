package net.anfoya.mail.browser.javafx.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import net.anfoya.java.io.SerializedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class Settings implements Serializable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
	private static final String FILENAME = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-settings";

	private static final Settings SETTINGS = new Settings();
	public static Settings getSettings() {
		return SETTINGS;
	}

	private final BooleanProperty showToolbar;

	public Settings() {
		showToolbar = new SimpleBooleanProperty(true);
	}

	public BooleanProperty showToolbar() {
		return showToolbar;
	}

	public static void load() {
		final Set<Object> s;
		try {
			s = new SerializedFile<Set<Object>>(FILENAME).load();
		} catch (final FileNotFoundException e) {
			LOGGER.warn("no settings found {}", FILENAME);
			return;
		} catch (final Exception e) {
			LOGGER.error("loading settings {}", FILENAME, e);
			return;
		}

		final Iterator<Object> i = s.iterator();
		SETTINGS.showToolbar.set((boolean) i.next());
	}

	public void save() {
		final Set<Object> s = new LinkedHashSet<Object>();

		s.add(showToolbar.get());

		try {
			new SerializedFile<Set<Object>>(FILENAME).save(s);
		} catch (final IOException e) {
			LOGGER.error("saving settings {}", FILENAME, e);
		}
	}
}
