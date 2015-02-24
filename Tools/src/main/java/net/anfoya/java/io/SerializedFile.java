package net.anfoya.java.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class SerializedFile<K> extends File {
	private static final Logger LOGGER = LoggerFactory.getLogger(SerializedFile.class);

	public SerializedFile(final String filepath) {
		super(filepath);
	}

	@SuppressWarnings("unchecked")
	public K load() throws ClassNotFoundException, IOException {
		LOGGER.info("loading {}", this);
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(this));
			return (K) new ObjectInputStream(bis).readObject();
		} finally {
			try {
				bis.close();
			} catch (final Exception e) {}
		}
	}

	public void save(final K k) throws IOException {
		LOGGER.info("saving {}", this);

		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(this));
			new ObjectOutputStream(bos).writeObject(k);
		} finally {
			try {
				bos.close();
			} catch (final Exception e) {}
		}
	}

	public boolean isOlder(final int field, final int value) {
		try {
			final Calendar refDate = Calendar.getInstance();
			refDate.add(field, -1 * value);
			return lastModified() < refDate.getTimeInMillis();
		} catch (final Exception e) {
			return true;
		}
	}
}