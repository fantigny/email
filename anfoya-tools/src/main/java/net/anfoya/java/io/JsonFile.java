package net.anfoya.java.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@SuppressWarnings("serial")
public class JsonFile<K> extends File {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonFile.class);

	public JsonFile(final String filepath) {
		super(filepath);
	}

	public JsonFile(final URI uri) throws URISyntaxException {
		super(uri);
	}

	public K load(final Type typeOfK) throws FileNotFoundException {
		LOGGER.info("loading {}", this);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(this));
			return new Gson().fromJson(reader, typeOfK);
		} finally {
			try {
				reader.close();
			} catch (final Exception e) {}
		}
	}

	public void save(final K k) throws IOException {
		LOGGER.info("saving {}", this);

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(this));
			new Gson().toJson(k, writer);
		} finally {
			try {
				writer.close();
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