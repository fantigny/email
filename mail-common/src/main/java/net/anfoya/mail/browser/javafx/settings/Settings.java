package net.anfoya.mail.browser.javafx.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.anfoya.java.io.SerializedFile;

@SuppressWarnings("serial")
public class Settings implements Serializable {

	public static final String URL = "fishermail.wordpress.com";
	public static final String VERSION_FILEPATH = "/version.txt";
	public static final String VERSION_URL = "http://81.108.162.255/version.txt";

	private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
	private static final String FILENAME = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-settings";

	private static final Settings SETTINGS = new Settings();
	public static Settings getSettings() {
		return SETTINGS;
	}

	private final BooleanProperty showToolbar;
	private final BooleanProperty showExcludeBox;
	private final BooleanProperty archiveOnDrop;
	private final IntegerProperty popupLifetime;
	private final BooleanProperty replyAllDblClick;
	private final StringProperty htmlSignature;

	public Settings() {
		showToolbar = new SimpleBooleanProperty(true);
		showExcludeBox = new SimpleBooleanProperty(false);
		archiveOnDrop = new SimpleBooleanProperty(true);
		popupLifetime = new SimpleIntegerProperty(20);
		replyAllDblClick = new SimpleBooleanProperty(false);
		htmlSignature = new SimpleStringProperty("");
	}

	public void load() {
		final List<Object> list;
		try {
			list = new SerializedFile<List<Object>>(FILENAME).load();
		} catch (final FileNotFoundException e) {
			LOGGER.warn("no settings found {}", FILENAME);
			return;
		} catch (final Exception e) {
			LOGGER.error("loading settings {}", FILENAME, e);
			return;
		}

		final Iterator<Object> i = list.iterator();
		if (i.hasNext()) {
			showToolbar.set((boolean) i.next());
		}
		if (i.hasNext()) {
			showExcludeBox.set((boolean) i.next());
		}
		if (i.hasNext()) {
			archiveOnDrop.set((boolean) i.next());
		}
		if (i.hasNext()) {
			popupLifetime.set((int) i.next());
		}
		if (i.hasNext()) {
			htmlSignature.set((String) i.next());
		}
		if (i.hasNext()) {
			replyAllDblClick.set((Boolean) i.next());
		}
	}

	public void save() {
		final List<Object> list = new ArrayList<Object>();

		list.add(showToolbar.get());
		list.add(showExcludeBox.get());
		list.add(archiveOnDrop.get());
		list.add(popupLifetime.get());
		list.add(htmlSignature.get());
		list.add(replyAllDblClick.get());

		try {
			new SerializedFile<List<Object>>(FILENAME).save(list);
		} catch (final IOException e) {
			LOGGER.error("saving settings {}", FILENAME, e);
		}
	}

	public BooleanProperty showToolbar() {
		return showToolbar;
	}

	public BooleanProperty showExcludeBox() {
		return showExcludeBox;
	}

	public BooleanProperty archiveOnDrop() {
		return archiveOnDrop;
	}

	public IntegerProperty popupLifetime() {
		return popupLifetime;
	}

	public BooleanProperty replyAllDblClick() {
		return replyAllDblClick;
	}

	public StringProperty htmlSignature() {
		return htmlSignature;
	}
}
