package net.anfoya.easylist.model;

import java.util.List;

public class ContainsWildcard extends Contains {

	private final StartsWildcard wildcard;

	public ContainsWildcard(final List<String> parts) {
		super(parts);
		wildcard = new StartsWildcard(parts);
	}

	@Override
	public boolean applies(final String url) {
		return wildcard.applies(url) && super.applies(url);
	}
}
