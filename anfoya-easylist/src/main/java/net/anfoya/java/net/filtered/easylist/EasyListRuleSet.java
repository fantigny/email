package net.anfoya.java.net.filtered.easylist;

// https://adblockplus.org/en/filters

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.cache.LocalCache;
import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.net.filtered.easylist.loader.InternetLoader;
import net.anfoya.java.net.filtered.easylist.model.Rule;
import net.anfoya.java.net.url.filter.RuleSet;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;

public class EasyListRuleSet implements RuleSet {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyListRuleSet.class);
	private static final Config CONFIG = new Config();

	// cache to store most commons URLs
	private static final LocalCache<String, Boolean> URL_EXCEPTIONS_CACHE = new LocalCache<String, Boolean>("easylist_exceptions", 1500);
	private static final LocalCache<String, Boolean> URL_EXCLUSIONS_CACHE = new LocalCache<String, Boolean>("easylist_exclusions", 1500);

	// process time usage statistics
	private static final AtomicLong PROCESS_TIME = new AtomicLong(0);

	// hit statistics
	private static final AtomicLong FILTER_HIT = new AtomicLong(0);
	private static final AtomicLong CACHE_HIT = new AtomicLong(0);
	private static final AtomicLong NB_REQUEST = new AtomicLong(0);

	static {
		new Timer("easylist-loader", true).schedule(new TimerTask() {
			private long time = System.nanoTime();
			@Override
			public void run() {
				final long time = System.nanoTime();
				if (NB_REQUEST.get() != 0) {
					LOGGER.info("filter {}%, cache {}%, cpu {}%"
							, (int) FILTER_HIT.get() / (double)NB_REQUEST.get() * 100.0
							, (int) CACHE_HIT.get() / (double)NB_REQUEST.get() * 100.0
							, (int) PROCESS_TIME.getAndSet(0) / (time - this.time) * 100.0);
				}
				this.time = time;
			}
		}, 0, 5 * 60 * 1000);
	}

	private final String[] internetUrls;

	private final Set<Rule> exceptions;
	private final Set<Rule> exclusions;

	private boolean withException;

	public EasyListRuleSet(final boolean withException) {
		this.internetUrls = CONFIG.getUrls();

		this.exceptions = new CopyOnWriteArraySet<Rule>();
		this.exclusions = new CopyOnWriteArraySet<Rule>();

		this.withException = withException;

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			URL_EXCEPTIONS_CACHE.save();
			URL_EXCLUSIONS_CACHE.save();
		}));
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

	public void add(final Rule rule) {
		switch (rule.getType()) {
		case exception:
			LOGGER.debug("added {}", rule);
			exceptions.add(rule);
			break;
		case exclusion:
			LOGGER.debug("added {}", rule);
			exclusions.add(rule);
			break;
		case empty:
			break;
		}
	}

	public void addAll(final EasyListRuleSet easyList) {
		exceptions.addAll(easyList.exceptions);
		exclusions.addAll(easyList.exclusions);
	}

	public void replaceAll(final EasyListRuleSet easyList) {
		exceptions.clear();
		exclusions.clear();
		addAll(easyList);
	}

	@Override
	public void load() {
		final SerializedFile<Set<Rule>> local = new SerializedFile<Set<Rule>>(CONFIG.getExceptionsFilePath());
		final Future<?> future = ThreadPool.getDefault().submit(PoolPriority.MAX, "load local rules", () -> {
			final long start = System.currentTimeMillis();
			exceptions.addAll(local.load());
			exclusions.addAll(new SerializedFile<Set<Rule>>(CONFIG.getExclusionsFilePath()).load());
			LOGGER.info("loaded {} local rules (in {}ms)", getRuleCount(), System.currentTimeMillis()-start);
			return null;
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, "load cache", () -> {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error("loading rule sets", e);
			}
			loadCache();
			if (local.isOlder(Calendar.DAY_OF_YEAR, 1)
					|| exceptions.isEmpty()
					|| exclusions.isEmpty()) {
				loadInternet();
			}
		});
	}

	private void loadCache() {
		final long start = System.currentTimeMillis();
		URL_EXCEPTIONS_CACHE.load();
		URL_EXCLUSIONS_CACHE.load();
		LOGGER.info("loaded {} URLs from cache (in {}ms)"
				, URL_EXCEPTIONS_CACHE.size() + URL_EXCLUSIONS_CACHE.size()
				, System.currentTimeMillis() - start);
	}

	protected void loadInternet() {
		final EasyListRuleSet internetList = new EasyListRuleSet(false);
		for(final String url: internetUrls) {
			try {
				// add to incremental list
				internetList.addAll(new InternetLoader(new URL(url)).load());
				// set as current list
				replaceAll(internetList);
			} catch (final Exception e) {
				LOGGER.error("loading {}", url, e);
			}
		}
		save();
	}

	private void save() {
		try {
			new SerializedFile<Set<Rule>>(CONFIG.getExceptionsFilePath()).save(exceptions);
		} catch (final IOException e) {
			LOGGER.error("saving exceptions", e);
		}
		try {
			new SerializedFile<Set<Rule>>(CONFIG.getExclusionsFilePath()).save(exclusions);
		} catch (final IOException e) {
			LOGGER.error("saving exclusions", e);
		}
	}

	@Override
	public boolean matchesException(final String url) {
		return matches(url, URL_EXCEPTIONS_CACHE, exceptions);
	}

	@Override
	public boolean matchesExclusion(final String url) {
		return matches(url, URL_EXCLUSIONS_CACHE, exclusions);
	}

	private boolean matches(final String url, final LocalCache<String, Boolean> urlCache, final Set<Rule> rules) {
		final long timer = System.nanoTime();
		NB_REQUEST.incrementAndGet();
		Boolean match = urlCache.get(url);
		if (match != null) {
			CACHE_HIT.incrementAndGet();
		} else {
			match = matches(url, rules);
			urlCache.put(url, match);
		}
		if (match) {
			FILTER_HIT.incrementAndGet();
		}
		PROCESS_TIME.addAndGet(System.nanoTime() - timer);

		return match;
	}

	private boolean matches(final String url, final Set<Rule> rules) {
		for(final Rule rule: rules) {
			if (rule.applies(url)) {
				LOGGER.debug("{} \"{}\" matches \"{}\" (regex={}) (original line={})"
						, rule.getType()
						, rule.getEffectiveLine()
						, url
						, rule.getRegex()
						, rule.getLine());
				return true;
			}
		}

		return false;
	}
}
