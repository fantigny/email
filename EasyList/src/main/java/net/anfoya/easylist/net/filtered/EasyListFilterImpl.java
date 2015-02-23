package net.anfoya.easylist.net.filtered;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

	private final EasyList delegate;

	public EasyListFilterImpl() {
		delegate = new EasyList(true);
	}

	@Override
	public void setWithException(final boolean withException) {
		delegate.setWithException(withException);
	}

	@Override
	public boolean isWithException() {
		return delegate.isWithException();
	}

	@Override
	public void loadFilters() {
		final Local local = new Local(new File(EASYLIST_FILEPATH));
		final Future<?> futureLocal = ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				final EasyList easyList = local.load();
				EasyListFilterImpl.this.delegate.addAll(easyList);
			}
		});

		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					futureLocal.get();
				} catch (InterruptedException | ExecutionException e) {
					LOGGER.error("loading {}", EASYLIST_FILEPATH, e);
					return;
				}
				if (delegate.isEmpty() || local.isOutdated()) {
					final EasyList easyList = new EasyList(true);
					for(final String urlStr: EASY_LIST_URLS) {
						try {
							final URL url = new URL(urlStr);
							easyList.addAll(new Internet(url).load());
							EasyListFilterImpl.this.delegate.replaceAll(easyList);
						} catch (final Exception e) {
							LOGGER.error("loading {}", urlStr, e);
						}
					}
					local.save(easyList);
				}
			}
		});
	}

	@Override
	public boolean matches(final URL url) {
		return delegate.matches(url.toString());
	}
}
