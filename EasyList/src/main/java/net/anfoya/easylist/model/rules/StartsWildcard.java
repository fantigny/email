package net.anfoya.easylist.model.rules;

import java.util.regex.Pattern;

public class StartsWildcard extends Rule {
	public static final String TERM = "||";
	private static final String REG_EX = "https?:\\/\\/.*\\.%s.*";

	private final Pattern regex;

	public StartsWildcard(final String line) {
		super(line.substring(TERM.length()));
		regex = Pattern.compile(String.format(REG_EX, Pattern.quote(getParts().get(0))));
	}

	@Override
	public boolean applies(final String url) {
		return regex.matcher(url).matches();
	}
}
