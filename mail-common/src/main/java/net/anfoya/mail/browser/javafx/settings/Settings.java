package net.anfoya.mail.browser.javafx.settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.service.MailService;

@SuppressWarnings("serial")
public class Settings implements Serializable {
	public static final String DOWNLOAD_URL = "https://fishermail.wordpress.com/download/";

	public static final String VERSION_TXT_RESOURCE = "/version.txt";
	public static final String VERSION_TXT_URL = "https://www.dropbox.com/s/tpknt8yxfhnlwhm/version.txt?dl=1";

	private static final String SND_PATH = "/net/anfoya/mail/snd/";
	public static final String MP3_NEW_MAIL = Settings.class.getClass().getResource(SND_PATH + "new_mail.mp3").toExternalForm();
	public static final String MP3_TRASH = Settings.class.getClass().getResource(SND_PATH + "trash.mp3").toExternalForm();

	private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
	private static final String FILENAME = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-settings";

	private static final String PERSISTENT_ID = "FisherMail ## global settings ##";

	private final MailService<?, ?, ?, ?, ?> mailService;

	private final LongProperty date;

	private final BooleanProperty showToolbar;
	private final BooleanProperty showExcludeBox;
	private final BooleanProperty archiveOnDrop;
	private final IntegerProperty popupLifetime;
	private final BooleanProperty replyAllDblClick;
	private final StringProperty htmlSignature;
	private final BooleanProperty confirmOnQuit;
	private final BooleanProperty confirmOnSignout;
	private final BooleanProperty mute;
	private final BooleanProperty globalSettings;

	private final DoubleProperty firstDivider;
	private final DoubleProperty secondDivider;
	private final DoubleProperty windowWidth;
	private final DoubleProperty windowHeight;
	private final StringProperty browserMode;
	private final DoubleProperty windowX;
	private final DoubleProperty windowY;

	public Settings(MailService<?, ?, ?, ?, ?> mailService) {
		this.mailService = mailService;

		showToolbar = new SimpleBooleanProperty(true);
		showExcludeBox = new SimpleBooleanProperty(false);
		archiveOnDrop = new SimpleBooleanProperty(true);
		popupLifetime = new SimpleIntegerProperty(20);
		replyAllDblClick = new SimpleBooleanProperty(false);
		htmlSignature = new SimpleStringProperty("");
		confirmOnQuit = new SimpleBooleanProperty(true);
		confirmOnSignout = new SimpleBooleanProperty(true);
		mute = new SimpleBooleanProperty(false);
		globalSettings = new SimpleBooleanProperty(true);
		firstDivider = new SimpleDoubleProperty(200);
		secondDivider = new SimpleDoubleProperty(600);
		windowWidth = new SimpleDoubleProperty(1400);
		windowHeight = new SimpleDoubleProperty(800);
		browserMode = new SimpleStringProperty("FULL");
		windowX = new SimpleDoubleProperty(-1);
		windowY = new SimpleDoubleProperty(-1);
		date = new SimpleLongProperty();
	}

	public List<?> toList() {
		final List<Object> list = new ArrayList<Object>();
		Collections.addAll(list
				, showToolbar.get()
				, showExcludeBox.get()
				, archiveOnDrop.get()
				, popupLifetime.get()
				, htmlSignature.get()
				, replyAllDblClick.get()
				, confirmOnQuit.get()
				, confirmOnSignout.get()
				, mute.get()
				, globalSettings.get()
				, firstDivider.get()
				, secondDivider.get()
				, windowWidth.get()
				, windowHeight.get()
				, browserMode.get()
				, windowX.get()
				, windowY.get()
				, date.get()
				);

		return list;
	}

	public void fromList(List<?> list) {
		final Iterator<?> i = list.iterator();
		if (i.hasNext()) { showToolbar		.set((Boolean)	i.next()); }
		if (i.hasNext()) { showExcludeBox	.set((Boolean)	i.next()); }
		if (i.hasNext()) { archiveOnDrop	.set((Boolean)	i.next()); }
		if (i.hasNext()) { popupLifetime	.set((Integer)	i.next()); }
		if (i.hasNext()) { htmlSignature	.set((String)	i.next()); }
		if (i.hasNext()) { replyAllDblClick	.set((Boolean)	i.next()); }
		if (i.hasNext()) { confirmOnQuit	.set((Boolean)	i.next()); }
		if (i.hasNext()) { confirmOnSignout	.set((Boolean)	i.next()); }
		if (i.hasNext()) { mute				.set((Boolean)	i.next()); }
		if (i.hasNext()) { globalSettings	.set((Boolean)	i.next()); }
		if (i.hasNext()) { firstDivider		.set((Double) 	i.next()); }
		if (i.hasNext()) { secondDivider	.set((Double) 	i.next()); }
		if (i.hasNext()) { windowWidth		.set((Double) 	i.next()); }
		if (i.hasNext()) { windowHeight		.set((Double) 	i.next()); }
		if (i.hasNext()) { browserMode		.set((String) 	i.next()); }
		if (i.hasNext()) { windowX			.set((Double) 	i.next()); }
		if (i.hasNext()) { windowY			.set((Double) 	i.next()); }
		if (i.hasNext()) { date				.set((Long) 	i.next()); }
	}


	public void load() {
		try {
			fromList(new SerializedFile<List<Object>>(FILENAME).load());
		} catch (final FileNotFoundException e) {
			LOGGER.warn("no settings found {}", FILENAME);
		} catch (final Exception e) {
			LOGGER.error("load settings {}", FILENAME, e);
		}

		if (globalSettings.get()) {
			ThreadPool.getDefault().submit(PoolPriority.MIN, "load global settings", () -> {
				byte[] bytes;
				try {
					bytes = mailService.readBytes(PERSISTENT_ID);
				} catch (final Exception e) {
					LOGGER.error("load global settings", e);
					return;
				}
				try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
					final List<?> list = (List<?>) new ObjectInputStream(bis).readObject();
					final Settings temp = new Settings(mailService);
					temp.fromList(list);
					if (temp.date.greaterThan(date).get()) {
						fromList(list);
					}
				} catch (final Exception e) {
					LOGGER.error("read global settings", e);
				}
			});
		}
	}

	public void saveNow() {
		date.set(Calendar.getInstance().getTimeInMillis());

		try {
			new SerializedFile<List<?>>(FILENAME).save(toList());
		} catch (final IOException e) {
			LOGGER.error("save settings {}", FILENAME, e);
		}

		if (globalSettings.get()) {
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(bos)) {
				oos.writeObject(toList());
				mailService.persistBytes(PERSISTENT_ID, bos.toByteArray());
			} catch (final Exception e) {
				LOGGER.error("saving global settings", e);
			}
		}
	}

	public void saveLater() {
		ThreadPool.getDefault().submit(PoolPriority.MIN, "save global settings", () -> saveNow());
	}

	public void reset() {
		new Settings(mailService).saveNow();
		load();
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

	public BooleanProperty confirmOnQuit() {
		return confirmOnQuit;
	}

	public BooleanProperty confirmOnSignout() {
		return confirmOnSignout;
	}

	public BooleanProperty mute() {
		return mute;
	}

	public BooleanProperty globalSettings() {
		return globalSettings;
	}

	public DoubleProperty firstDivider() {
		return firstDivider;
	}

	public DoubleProperty secondDivider() {
		return secondDivider;
	}

	public DoubleProperty windowWidth() {
		return windowWidth;
	}

	public DoubleProperty windowHeight() {
		return windowHeight;
	}

	public StringProperty browserMode() {
		return browserMode;
	}

	public DoubleProperty windowX() {
		return windowX;
	}

	public DoubleProperty windowY() {
		return windowY;
	}
}
