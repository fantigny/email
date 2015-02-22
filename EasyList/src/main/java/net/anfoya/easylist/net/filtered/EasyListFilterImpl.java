package net.anfoya.easylist.net.filtered;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import net.anfoya.easylist.loader.Internet;
import net.anfoya.easylist.loader.Local;
import net.anfoya.easylist.model.EasyList;
import net.anfoya.tools.net.filtered.UrlFilter;
import net.anfoya.tools.util.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyListFilterImpl implements UrlFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyListFilterImpl.class);

	private static final String EASYLIST_FILEPATH = System.getProperty("java.io.tmpdir") + "/easylist.json";
	private static final String[] EASY_LIST_URLS = {
		"https://easylist-downloads.adblockplus.org/easylist.txt"
		, "https://easylist-downloads.adblockplus.org/liste_fr.txt"
	};

	private final EasyList easyList;

	public EasyListFilterImpl() {
		easyList = new EasyList();
	}

	@Override
	public void loadFilters() {
		final File file = new File(EASYLIST_FILEPATH);
		Local local = null;
		if (file.exists()) {
			local = new Local(file);
			easyList.add(local.load());
		}

		if (!file.exists() || easyList.isEmpty() || local.isOutdated()) {
			ThreadPool.getInstance().submit(new Runnable() {
				@Override
				public void run() {
					final EasyList easyList = new EasyList();
					for(final String urlStr: EASY_LIST_URLS) {
						try {
							final URL url = new URL(urlStr);
							easyList.add(new Internet(url).load());
							EasyListFilterImpl.this.easyList.clearAdd(easyList);
						} catch (final MalformedURLException e) {
							LOGGER.error("loading {}", urlStr, e);
						}
					}
					new Local(file).save(easyList);
				}
			});
		}
	}

	@Override
	public boolean filtered(final URL url) {
		return easyList.applies(url.toString());
	}
}
