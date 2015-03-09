package net.anfoya.java.net.filtered.easylist;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import net.anfoya.java.net.filtered.easylist.loader.Internet;
import net.anfoya.java.net.filtered.easylist.loader.Local;
import net.anfoya.java.net.filtered.easylist.model.Rule;
import net.anfoya.java.net.filtered.engine.RuleSet;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class EasyListFilterImpl implements RuleSet, Serializable {
	private static final transient Cache<String> EXCEPTION_CACHE = new Cache<String>("exception", 500000);
	private static final transient Cache<String> EXCLUSION_CACHE = new Cache<String>("exclusion", 500000);

	private static final Logger LOGGER = LoggerFactory.getLogger(EasyListFilterImpl.class);
	private static final AtomicLong LOGGER_TIMER = new AtomicLong(System.currentTimeMillis());
	private static final AtomicLong TIMER = new AtomicLong(0);
	private static final AtomicLong TOTAL = new AtomicLong(0);
	private static final AtomicLong CACHED = new AtomicLong(0);
	private static final AtomicLong HIT = new AtomicLong(0);

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
			LOGGER.debug("Exception added {}", rule);
			exceptions.add(rule);
			break;
		case exclusion:
			LOGGER.debug("Exclusion added {}", rule);
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
				EXCEPTION_CACHE.load();
				EXCLUSION_CACHE.load();

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
					final EasyListFilterImpl internetList = new EasyListFilterImpl(true);
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
				}
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						EXCEPTION_CACHE.save();
						EXCLUSION_CACHE.save();
					}
				}));
			}
		});
	}

	@Override
	public boolean matchesException(final String url) {
		return matches(exceptions, url, EXCEPTION_CACHE);
	}

	@Override
	public boolean matchesExclusion(final String url) {
		return matches(exclusions, url, EXCLUSION_CACHE);
	}

	private boolean matches(final Set<Rule> rules, final String url, final List<String> cache) {
		TOTAL.incrementAndGet();
		long timer = System.nanoTime();
		try {
			if (cache.contains(url)) {
				timer = System.nanoTime() - timer;
				CACHED.incrementAndGet();
				return false;
			}
			for(final Rule rule: rules) {
				if (rule.applies(url)) {
					LOGGER.debug("{} \"{}\" matches \"{}\" (regex={}) (original line={})"
							, rule.getType()
							, rule.getEffectiveLine()
							, url
							, rule.getRegex()
							, rule.getLine());
					timer = System.nanoTime() - timer;
					HIT.incrementAndGet();
					return true;
				}
			}
			cache.add(url);
			timer = System.nanoTime() - timer;
			return false;
		} finally {
			TIMER.addAndGet(timer);
			if (LOGGER_TIMER.get() + 1000 < System.currentTimeMillis()) {
				LOGGER_TIMER.set(System.currentTimeMillis());
				LOGGER.info("processing time {}s with {}% success ({}/{}), {}% cached"
						, TIMER.get() / 1000000000
						, (int)((double)HIT.get() / (double)TOTAL.get() * 100.0)
						, HIT.get()
						, TOTAL.get()
						, (int)((double)CACHED.get() / (double)TOTAL.get() * 100.0));
			}
		}
	}
}
