package net.anfoya.easylist.model;

import java.util.List;

public abstract class Rule {
	private final List<String> parts;

	public Rule(final List<String> parts) {
		this.parts = parts;
	}

	public abstract boolean applies(String url);

	public List<String> getParts() {
		return parts;
	}
}
