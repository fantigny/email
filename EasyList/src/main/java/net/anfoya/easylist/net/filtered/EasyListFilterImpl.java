package net.anfoya.easylist.net.filtered;

import java.io.File;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

import net.anfoya.easylist.loader.Internet;
import net.anfoya.easylist.loader.Local;
import net.anfoya.easylist.model.Config;
import net.anfoya.easylist.model.Rule;
import net.anfoya.tools.net.filtered.engine.RuleSet;
import net.anfoya.tools.util.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyListFilterImpl implements RuleSet {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyListFilterImpl.class);

	private final Config config;

	private final Set<Rule> exceptions;
	private final Set<Rule> exclusions;

	private boolean withException;

	public EasyListFilterImpl(final boolean withException) {
		this.config = new Config();
		this.withException = withException;
		exceptions = new CopyOnWriteArraySet<Rule>();
		exclusions = new CopyOnWriteArraySet<Rule>();
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
		exceptions.addAll(easyList.exceptions);
		exclusions.clear();
		exclusions.addAll(easyList.exclusions);
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

	public void addAll(final EasyListFilterImpl easylist) {
		exceptions.addAll(easylist.exceptions);
		exclusions.addAll(easylist.exclusions);
	}

	@Override
	public void load() {
		final Local local = new Local(new File(config.getFilePath()));
		final Future<?> futureLocal = ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				final EasyListFilterImpl localList = local.load();
				addAll(localList);
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
				if (isEmpty() || local.isOutdated()) {
					final EasyListFilterImpl internetList = new EasyListFilterImpl(true);
					for(final String url: config.getUrls()) {
						try {
							internetList.addAll(new Internet(new URL(url)).load());
							replaceAll(internetList);
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
	public boolean matchesException(final String url) {
		return matches(exceptions, url);
	}

	@Override
	public boolean matchesExclusion(final String url) {
		return matches(exclusions, url);
	}

	private boolean matches(final Set<Rule> rules, final String url) {
		for(final Rule rule: rules) {
			if (rule.applies(url)) {
				LOGGER.info("{} \"{}\" matches \"{}\" (regex={}) (original line={})"
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
