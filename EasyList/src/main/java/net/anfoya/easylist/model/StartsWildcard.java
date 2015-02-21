package net.anfoya.easylist.model;

import java.util.List;
import java.util.regex.Pattern;

public class StartsWildcard extends Rule {
	private static final String REG_EX = "https?:\\/\\/.*\\.";

	private final Pattern regex;

	public StartsWildcard(final List<String> parts) {
		super(parts);
		regex = Pattern.compile(REG_EX + getParts().get(0));
	}

	@Override
	public boolean applies(final String url) {
		return regex.matcher(url).matches();
	}
}
