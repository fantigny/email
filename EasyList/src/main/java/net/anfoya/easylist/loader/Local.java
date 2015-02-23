package net.anfoya.easylist.loader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import net.anfoya.easylist.model.EasyList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;


public class Local {
	private static final Logger LOGGER = LoggerFactory.getLogger(Local.class);

	private final File file;

	public Local(final File file) {
		this.file = file;
	}

	public EasyList load() {
		LOGGER.info("loading {}", file);
		EasyList easyList;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			easyList = new Gson().fromJson(reader, EasyList.class);
		} catch (final Exception e) {
			if (e instanceof FileNotFoundException) {
				LOGGER.warn("reading {} ({})", file, e.getMessage());
			} else {
				LOGGER.error("reading {}", file, e);
			}
			easyList = new EasyList(true);
		} finally {
			try {
				reader.close();
			} catch (final Exception e) {}
		}

		LOGGER.info("loaded {} rules", easyList.getRuleCount());
		return easyList;
	}

	public void save(final EasyList easyList) {
		LOGGER.info("saving {} rules to {}", easyList.getRuleCount(), file);

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			new Gson().toJson(easyList, writer);
		} catch (final IOException e) {
			LOGGER.warn("writing {}", file, e);
		} finally {
			try {
				writer.close();
			} catch (final Exception e) {}
		}
	}

	public boolean isOutdated() {
		try {
			final Calendar today = Calendar.getInstance();
			today.set(Calendar.HOUR, 0);
			today.set(Calendar.MINUTE, 0);
			today.set(Calendar.SECOND, 0);
			return file.lastModified() < today.getTimeInMillis();
		} catch (final Exception e) {
			return true;
		}
	}

	public boolean exists() {
		return file.exists();
	}
}
