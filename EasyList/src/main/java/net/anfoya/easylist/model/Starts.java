package net.anfoya.easylist.model;

import java.util.List;

public class Starts extends Rule {

	private final String start;

	public Starts(final List<String> parts) {
		super(parts);
		start = parts.get(0);
	}

	@Override
	public boolean applies(final String url) {
		return url.startsWith(start);
	}
}
