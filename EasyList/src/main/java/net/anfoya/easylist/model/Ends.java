package net.anfoya.easylist.model;

import java.util.List;

public class Ends extends Rule {

	private final String end;

	public Ends(final List<String> parts) {
		super(parts);
		end = parts.get(0);
	}

	@Override
	public boolean applies(final String url) {
		return url.endsWith(end);
	}
}
