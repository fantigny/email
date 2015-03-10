package net.anfoya.java.net.filtered.easylist;

import java.io.Serializable;
import java.net.URL;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import net.anfoya.java.net.filtered.easylist.cache.LocalCache;
import net.anfoya.java.net.filtered.easylist.loader.Internet;
import net.anfoya.java.net.filtered.easylist.loader.Local;
import net.anfoya.java.net.filtered.easylist.model.Rule;
import net.anfoya.java.net.filtered.engine.RuleSet;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class EasyListFilterImpl implements RuleSet, Serializable {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyListFilterImpl.class);

	private static final long REF_TIME = System.nanoTime();
	private static final AtomicLong LOGGER_TIMER = new AtomicLong(System.currentTimeMillis());
	private static final AtomicLong TIMER = new AtomicLong(0);
	private static final AtomicLong TOTAL = new AtomicLong(0);
	private static final AtomicLong CACHED = new AtomicLong(0);
	private static final AtomicLong HIT = new AtomicLong(0);

	private static final transient LocalCache<String, Boolean> URL_EXCEP_CACHE = new LocalCache<String, Boolean>("exception", 100);
	private static final transient LocalCache<String, Boolean> URL_EXCLU_CACHE = new LocalCache<String, Boolean>("exclusion", 100);

	private final String localFilepath;
	private final String[] internetUrls;

	private final Set<Rule> exceptions;
	private final Set<Rule> exclusions;

	private boolean withException;

	public EasyListFilterImpl(final boolean withException) {
		final Config config = new Config();
		this.localFilepath = config.getFilePath();
		this.internetUrls = config.getUrls();

		this.exceptions = new CopyOnWriteArraySet<Rule>();
		this.exclusions = new CopyOnWriteArraySet<Rule>();

		this.withException = withException;
	}

	@Override
	public boolean isWithException() {
		return withException;
	}

	@Override
	public void setWithException(final boolean withException) {
		this.withException = withException;
	}

	public int getRuleCount() {
		return exclusions.size() + exceptions.size();
	}

	public boolean isEmpty() {
		return getRuleCount() == 0;
	}

	public void replaceAll(final EasyListFilterImpl easyList) {
		exceptions.clear();
		exclusions.clear();
		addAll(easyList);
	}

	public void add(final Rule rule) {
		switch (rule.getType()) {
		case exception:
			LOGGER.debug("exception added {}", rule);
			exceptions.add(rule);
			break;
		case exclusion:
			LOGGER.debug("exclusion added {}", rule);
			exclusions.add(rule);
			break;
		case empty:
			break;
		}
	}

	public void addAll(final EasyListFilterImpl easyList) {
		exceptions.addAll(easyList.exceptions);
		exclusions.addAll(easyList.exclusions);
	}

	@Override
	public void load() {
		final Local local = new Local(localFilepath);
		final Future<?> futureLocal = ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				URL_EXCEP_CACHE.load();
				URL_EXCLU_CACHE.load();

				final EasyListFilterImpl localList = local.load();
				setWithException(localList.isWithException());
				addAll(localList);
			}
		});

		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					futureLocal.get();
				} catch (final Exception e) {
					LOGGER.error("loading {}", localFilepath, e);
					return;
				}
				if (isEmpty() || local.isOutdated()) {
					final EasyListFilterImpl internetList = new EasyListFilterImpl(false);
					for(final String url: internetUrls) {
						try {
							// add to incremental list
							internetList.addAll(new Internet(new URL(url)).load());
							// set as current list
							replaceAll(internetList);
						} catch (final Exception e) {
							LOGGER.error("loading {}", url, e);
						}
					}
					local.save(internetList);
					if (URL_EXCEP_CACHE.isOlder(Calendar.DAY_OF_YEAR, 7)) {
						URL_EXCEP_CACHE.clear();
						URL_EXCLU_CACHE.clear();
					}
				}
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						URL_EXCEP_CACHE.save();
						URL_EXCLU_CACHE.save();
					}
				}));
			}
		});
	}

	@Override
	public boolean matchesException(final String url) {
		return matches(url, URL_EXCEP_CACHE, exceptions);
	}

	@Override
	public boolean matchesExclusion(final String url) {
		return matches(url, URL_EXCLU_CACHE, exclusions);
	}

	private boolean matches(final String url, final LocalCache<String, Boolean> urlCache, final Set<Rule> rules) {
		TOTAL.incrementAndGet();

		boolean match;
		long timer = System.nanoTime();

		final Boolean cachedMatches = urlCache.get(url);
		if (cachedMatches != null) {
			CACHED.incrementAndGet();
			match = cachedMatches;
		} else {
			match = false;
			for(final Rule rule: rules) {
				if (rule.applies(url)) {
					LOGGER.debug("{} \"{}\" matches \"{}\" (regex={}) (original line={})"
							, rule.getType()
							, rule.getEffectiveLine()
							, url
							, rule.getRegex()
							, rule.getLine());
					match = true;
					break;
				}
			}
			urlCache.put(url, match);
		}
		if (match) {
			HIT.incrementAndGet();
		}
		timer = TIMER.addAndGet(System.nanoTime() - timer);

		if (LOGGER_TIMER.get() + 3000 < System.currentTimeMillis()) {
			LOGGER_TIMER.set(System.currentTimeMillis());
			LOGGER.info("cpu {}%, cached {}%, success {}%"
					, (int)(timer / (double)(System.nanoTime() - REF_TIME) * 100.0)
					, (int)(CACHED.get() / (double)TOTAL.get() * 100.0)
					, (int)(HIT.get() / (double)TOTAL.get() * 100.0));
		}

		return match;
	}
}
