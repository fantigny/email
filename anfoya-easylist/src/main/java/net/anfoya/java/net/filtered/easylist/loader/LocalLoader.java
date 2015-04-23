package net.anfoya.java.net.filtered.easylist.loader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;

import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.net.filtered.easylist.EasyListRuleSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class LocalLoader extends SerializedFile<EasyListRuleSet> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalLoader.class);

	public LocalLoader(final String filepath) {
		super(filepath);
	}

	@Override
	public EasyListRuleSet load() {
		final long start = System.currentTimeMillis();
		EasyListRuleSet easyList = new EasyListRuleSet(false);
		try {
			easyList = super.load();
		} catch (final FileNotFoundException e) {
			LOGGER.warn("file not fount {}", this);
		} catch (final ClassNotFoundException e) {
			LOGGER.warn("wrong format {}", this);
		} catch (final IOException e) {
			LOGGER.warn("reading {}", this, e);
		}

		LOGGER.info("loaded {} rules (in {}ms)", easyList.getRuleCount(), System.currentTimeMillis()-start);
		return easyList;
	}

	@Override
	public void save(final EasyListRuleSet easyList) {
		LOGGER.info("saving {} rules", easyList.getRuleCount());

		try {
			super.save(easyList);
		} catch (final IOException e) {
			LOGGER.error("writing {}", this, e);
		}
	}

	public boolean isOutdated() {
		return isOlder(Calendar.DAY_OF_YEAR, 1);
	}
}
