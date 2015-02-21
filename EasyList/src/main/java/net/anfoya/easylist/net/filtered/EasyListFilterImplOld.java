package net.anfoya.easylist.net.filtered;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.anfoya.tools.net.filtered.UrlFilter;
import net.anfoya.tools.util.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class EasyListFilterImplOld implements UrlFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyListFilterImplOld.class);

	private static final String FILTERS_FILEPATH = System.getProperty("java.io.tmpdir") + "/url_filters.json";
	private static final String[] EASY_LIST_URLS = {
		"https://easylist-downloads.adblockplus.org/easylist.txt"
		, "https://easylist-downloads.adblockplus.org/liste_fr.txt"
	};

	private final Set<List<String>> filters = new CopyOnWriteArraySet<List<String>>();

	@Override
	public boolean filtered(final URL url) {
		final String urlStr = url.toString();
		for(final List<String> urlElements: filters) {
			boolean filtered = true;
			for(final String urlElement: urlElements) {
				if (!urlStr.contains(urlElement)) {
					filtered = false;
					break;
				}
			}
			if (filtered) {
				LOGGER.debug("url filtered {} by {}", urlStr, urlElements);
				return true;
			}
		}

		return false;
	}

	@Override
	public void loadFilters() {
		load();
		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				download();
			}
		});
	}

	private void load() {
		File file = new File(FILTERS_FILEPATH);
		if (!file.exists()) {
			return;
		}
		
		LOGGER.info("loading {}", FILTERS_FILEPATH);
		String json;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			json = reader.readLine();
		} catch (final IOException e) {
			LOGGER.warn("reading {}", FILTERS_FILEPATH, e);
			json = null;
		} finally {
			try {
				reader.close();
			} catch (final Exception e) {}
		}

		Set<List<String>> filters = new Gson().fromJson(json, new TypeToken<Set<List<String>>>(){}.getType());
		LOGGER.info("loaded {} filters", filters.size());
		this.filters.addAll(filters);
	}

	private void download() {
		if (downloadRequired()) {
			for(String url: EASY_LIST_URLS) {
				download(url);
				saveFilters();
			}
		}
	}
	
	private void download(String url) {
		LOGGER.info("loading {}", url);
		try {
			// avoid handler factory re-entrance
			URLStreamHandler handler = url.startsWith("https://")
					? new sun.net.www.protocol.https.Handler()
					: new sun.net.www.protocol.http.Handler();
			InputStream in = new URL(null, url, handler).openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			String line;
			final Set<List<String>> filters = new HashSet<List<String>>();
			while((line=reader.readLine()) != null) {
				final List<String> filter = parseFilter(line);
				if (!filter.isEmpty()) {
					filters.add(filter);
				}
			}
			LOGGER.info("loaded {} filters", filters.size());
			this.filters.addAll(filters);
			LOGGER.info("total filters: {}", this.filters.size());
		} catch (final IOException e) {
			LOGGER.error("reading {}", url, e);
		}
	}

	private boolean downloadRequired() {
		File file = new File(FILTERS_FILEPATH);
		if (!file.exists()) {
			return true;
		}
		
		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		return file.lastModified() < today.getTimeInMillis();
	}

	private void saveFilters() {
		LOGGER.info("saving {}", FILTERS_FILEPATH);
		String json = new Gson().toJson(filters);
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(FILTERS_FILEPATH));
			writer.write(json);
		} catch (final IOException e) {
			LOGGER.warn("writing {}", FILTERS_FILEPATH, e);
		} finally {
			try {
				writer.close();
			} catch (final Exception e) {}
		}
	}

	private List<String> parseFilter(final String line) {
		final List<String> filter = new ArrayList<String>();

		if (line.isEmpty()) {
			return filter;
		}
		final char firstChar = line.charAt(0);
		if (firstChar == '[' || firstChar == '!' || line.contains("#")) {
			return filter;
		}

		if (firstChar == '|' && line.contains("||")) {
			return getUrlElements(line.substring(2));
		}
		if (firstChar == '@' && line.contains("@@||")) {
			return getUrlElements(line.substring(4));
		}

		return filter;
	}

	private List<String> getUrlElements(String line) {
		final List<String> urlElements = new ArrayList<String>();
		if (line.contains("^")) {
			final String[] elements = line.split("\\^");
			urlElements.add(elements[0]);
			line = elements.length > 1? elements[1]: "";
		}
		if (line.contains("*")) {
			final String[] elements = line.split("\\*");
			urlElements.addAll(Arrays.asList(elements));
		}
		if (line.contains("$")) {
			final String[] elements = line.split("\\$");
			urlElements.add(elements[0]);
		}

		return urlElements;
	}
}