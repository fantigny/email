package net.anfoya.java.net.filtered.easylist;

import java.io.Serializable;
import java.net.URL;
import java.util.Calendar;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import net.anfoya.java.cache.LocalCache;
import net.anfoya.java.net.filtered.easylist.loader.Internet;
import net.anfoya.java.net.filtered.easylist.loader.Local;
import net.anfoya.java.net.filtered.easylist.model.Rule;
import net.anfoya.java.net.filtered.engine.RuleSet;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO: serialize only data structure (not all object)

@SuppressWarnings("serial")
public class EasyListFilterImpl implements RuleSet, Serializable {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyListFilterImpl.class);

	// process time usage statistics
	private static final AtomicLong PROCESS_TIME = new AtomicLong(0);

	// hit statistics
	private static final AtomicLong FILTER_HIT = new AtomicLong(0);
	private static final AtomicLong CACHE_HIT = new AtomicLong(0);
	private static final AtomicLong NB_REQUEST = new AtomicLong(0);

	private static final transient LocalCache<String, Boolean> URL_EXCEP_CACHE = new LocalCache<String, Boolean>("exception", 1500);
	private static final transient LocalCache<String, Boolean> URL_EXCLU_CACHE = new LocalCache<String, Boolean>("exclusion", 1500);

	static {
		new Timer(true).schedule(new TimerTask() {
			private double filter = -1;
			private double hit = -1;
			private double process = -1;
			private long time = System.nanoTime();
			@Override
			public void run() {
				final long time = System.nanoTime();
				if (NB_REQUEST.get() != 0) {
					double filter = FILTER_HIT.getAndSet(0) / (double)NB_REQUEST.get() * 100.0;
					if (this.filter != -1) {
						filter = (filter * 2d + this.filter) / 3d;
					}
					this.filter = filter;
					double cache = CACHE_HIT.getAndSet(0) / (double)NB_REQUEST.getAndSet(0) * 100.0;
					if (this.hit != -1) {
						cache = (cache * 2d + this.hit) / 3d;
					}
					this.hit = cache;
					double process = PROCESS_TIME.getAndSet(0) / (time - this.time) * 100.0;
					if (this.process != -1) {
						process = (process * 2d + this.process) / 3d;
					}
					this.process = process;
					LOGGER.info("filter {}%, cache {}%, cpu {}%"
							, (int) filter
							, (int) cache
							, (int) process);
				} else {
					this.filter = 0;
					this.hit = 0;
					this.process = 0;
				}
				this.time = time;
			}
		}, 0, 3000);
	}

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
						CACHE_HIT.set(0);
						FILTER_HIT.set(0);
						NB_REQUEST.set(0);
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
