package net.anfoya.easylist.model.rules;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class Rule {
	private final String line;

	public Rule() {
		this(null);
	}

	public Rule(final String line) {
		this.line = line == null? "": line;
	}

	public boolean applies(final String url) {
		throw new NotImplementedException();
	}

	public String getLine() {
		return line;
	}

	public String getStart() {
		return getParts()[0];
	}

	public String getEnds() {
		final String[] parts = getParts();
		return parts[parts.length-1];
	}

	@Override
	public String toString() {
		return String.format("%s \"%s\""
				, getClass().getSimpleName()
				, getLine());
	}

	public boolean isEmpty() {
		return getLine().isEmpty();
	}

	public String[] getParts() {
		return getLine().split("[\\^\\*]");
	}
}
