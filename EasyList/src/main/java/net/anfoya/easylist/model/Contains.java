package net.anfoya.easylist.model;

import java.util.List;

public class Contains extends Rule {

	public Contains(final List<String> parts) {
		super(parts);
	}

	@Override
	public boolean applies(final String url) {
		for(final String part: getParts()) {
			if (!url.contains(part)) {
				return false;
			}
		}
		return true;
	}
}
