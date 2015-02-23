package net.anfoya.easylist.loader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;

import net.anfoya.easylist.net.filtered.EasyListFilterImpl;
import net.anfoya.java.io.JsonFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("serial")
public class Local extends JsonFile<EasyListFilterImpl> {
	private static final Logger LOGGER = LoggerFactory.getLogger(Local.class);

	public Local(final String filepath) {
		super(filepath);
	}

	public EasyListFilterImpl load() {
		final long start = System.currentTimeMillis();
		EasyListFilterImpl easyList;
		try {
			easyList = load(EasyListFilterImpl.class);
		} catch (final FileNotFoundException e) {
			LOGGER.warn("file not fount {}", this);
			easyList = new EasyListFilterImpl(true);
		}

		LOGGER.info("loaded {} rules (in {}ms)", easyList.getRuleCount(), System.currentTimeMillis()-start);
		return easyList;
	}

	@Override
	public void save(final EasyListFilterImpl easyList) {
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
