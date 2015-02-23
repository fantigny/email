package net.anfoya.easylist.model;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyList {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyList.class);

	private final Set<Rule> exceptions;
	private final Set<Rule> contains;

	private boolean withException;

	public EasyList(boolean withException) {
		this.withException = withException;
		exceptions = new CopyOnWriteArraySet<Rule>();
		contains = new CopyOnWriteArraySet<Rule>();
	}
	
	public boolean isWithException() {
		return withException;
	}

	public void setWithException(boolean withException) {
		this.withException = withException;
	}

	public int getRuleCount() {
		return contains.size() + exceptions.size();
	}

	public void clearAdd(final EasyList easyList) {
		exceptions.clear();
		exceptions.addAll(easyList.exceptions);
		contains.clear();
		contains.addAll(easyList.contains);
	}

	public void add(final Rule rule) {
		if (!rule.isEmpty()) {
			if (rule.isException()) {
				LOGGER.debug("Exception added {}", rule);
				exceptions.add(rule);
			} else {
				LOGGER.debug("Contains added {}", rule);
				contains.add(rule);
			}
		}
	}

	public boolean applies(final String url) {
		if (withException) {
			for(final Rule exception: exceptions) {
				if (exception.applies(url)) {
					LOGGER.info("applied {} to \"{}\"", exception, url);
					return false;
				}
			}
		}

		for(final Rule contain: contains) {
			if (contain.applies(url)) {
				LOGGER.info("applied {} to \"{}\"", contain, url);
				return true;
			}
		}

		return false;
	}

	public boolean isEmpty() {
		return getRuleCount() == 0;
	}

	public void add(final EasyList easylist) {
		exceptions.addAll(easylist.exceptions);
		contains.addAll(easylist.contains);
	}
}
