package net.anfoya.easylist.model;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyList {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyList.class);

	private final Set<Rule> exceptions;
	private final Set<Rule> exclusions;

	private boolean withException;

	public EasyList(final boolean withException) {
		this.withException = withException;
		exceptions = new CopyOnWriteArraySet<Rule>();
		exclusions = new CopyOnWriteArraySet<Rule>();
	}

	public boolean isWithException() {
		return withException;
	}

	public void setWithException(final boolean withException) {
		this.withException = withException;
	}

	public int getRuleCount() {
		return exclusions.size() + exceptions.size();
	}

	public boolean isEmpty() {
		return getRuleCount() == 0;
	}

	public void replaceAll(final EasyList easyList) {
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

	public void addAll(final EasyList easylist) {
		exceptions.addAll(easylist.exceptions);
		exclusions.addAll(easylist.exclusions);
	}

	public boolean matches(final String url) {
		if (withException && matches(exceptions, url)) {
			// URL matches an exception rule
			return false;
		} else if (matches(exclusions, url)) {
			// URL matches an exclusion rule
			return true;
		} else {
			// no rule applies
			return false;
		}
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
