package net.anfoya.easylist.net.filtered;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Future;

import net.anfoya.easylist.loader.Internet;
import net.anfoya.easylist.loader.Local;
import net.anfoya.easylist.model.Config;
import net.anfoya.easylist.model.EasyList;
import net.anfoya.tools.net.filtered.UrlFilter;
import net.anfoya.tools.util.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyListFilterImpl implements UrlFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyListFilterImpl.class);

	private final Config config;
	private final EasyList delegate;

	public EasyListFilterImpl() {
		config = new Config();
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
	public void loadRules() {
		final Local local = new Local(new File(config.getFilePath()));
		final Future<?> futureLocal = ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				final EasyList localList = local.load();
				delegate.addAll(localList);
			}
		});

		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					futureLocal.get();
				} catch (final Exception e) {
					LOGGER.error("loading {}", config.getFilePath(), e);
					return;
				}
				if (delegate.isEmpty() || local.isOutdated()) {
					final EasyList internetList = new EasyList(true);
					for(final String url: config.getUrls()) {
						try {
							internetList.addAll(new Internet(new URL(url)).load());
							delegate.replaceAll(internetList);
						} catch (final Exception e) {
							LOGGER.error("loading {}", url, e);
						}
					}
					local.save(internetList);
				}
			}
		});
	}

	@Override
	public boolean matches(final URL url) {
		return delegate.matches(url.toString());
	}
}
