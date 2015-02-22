package net.anfoya.easylist.model;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.easylist.model.rules.Contains;
import net.anfoya.easylist.model.rules.ContainsHttpWildcard;
import net.anfoya.easylist.model.rules.EmptyRule;
import net.anfoya.easylist.model.rules.Exception;
import net.anfoya.easylist.model.rules.ExceptionHttpWildcard;
import net.anfoya.easylist.model.rules.Rule;

public class EasyList {
	private static final Logger LOGGER = LoggerFactory.getLogger(EasyList.class);

	private final Set<Rule> exceptions;
	private final Set<Rule> contains;

	public EasyList() {
		exceptions = new HashSet<Rule>();
		contains = new HashSet<Rule>();
	}

	public int getRuleCount() {
		return contains.size() + exceptions.size();
	}

	public void add(final EasyList easyList) {
		exceptions.addAll(easyList.exceptions);
		contains.addAll(easyList.contains);
	}

	public void clearAdd(final EasyList easyList) {
		synchronized (exceptions) {
			exceptions.clear();
			exceptions.addAll(easyList.exceptions);
		}
		synchronized (contains) {
			contains.clear();
			contains.addAll(easyList.contains);
		}
	}

	public void add(final Rule rule) {
		
		if (!(rule instanceof EmptyRule)) {
			if (rule instanceof Exception
					|| rule instanceof ExceptionHttpWildcard) {
				exceptions.add(rule);
			} else if (rule instanceof Contains
					|| rule instanceof ContainsHttpWildcard) {
				LOGGER.debug("*** rule added {}", rule);
				contains.add(rule);
			}
		}
	}

	public boolean applies(final String url) {
		for(final Rule exception: exceptions) {
			if (exception.applies(url)) {
				LOGGER.debug("++ exception applied {}", exception);
				return false;
			}
		}
		
		for(final Rule contain: contains) {
			if (contain.applies(url)) {
				LOGGER.debug("-- exclusion applied {}", contain);
				return true;
			}
		}
		
		return false;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return getRuleCount() == 0;
	}
}
