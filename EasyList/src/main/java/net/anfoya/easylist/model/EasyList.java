package net.anfoya.easylist.model;

import java.util.HashSet;
import java.util.Set;

import net.anfoya.easylist.model.rules.Contains;
import net.anfoya.easylist.model.rules.ContainsWildcard;
import net.anfoya.easylist.model.rules.EmptyRule;
import net.anfoya.easylist.model.rules.Exception;
import net.anfoya.easylist.model.rules.ExceptionWildcard;
import net.anfoya.easylist.model.rules.Rule;

public class EasyList {

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
		if (rule instanceof EmptyRule) {
			return;
		} else if (rule instanceof Exception
				|| rule instanceof ExceptionWildcard) {
			exceptions.add(rule);
		} else if (rule instanceof Contains
				|| rule instanceof ContainsWildcard) {
			contains.add(rule);
		}
	}

	public boolean applies(final String url) {
		for(final Rule exception: exceptions) {
			if (exception.applies(url)) {
				return false;
			}
		}
		for(final Rule contain: contains) {
			if (contain.applies(url)) {
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
