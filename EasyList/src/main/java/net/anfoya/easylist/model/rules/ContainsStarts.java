package net.anfoya.easylist.model.rules;

import java.util.regex.Pattern;


public class ContainsStarts extends Contains {
	public static final String TERM = "|";
	private static final String REG_EX = "^%s.*";

	private final Pattern regex;

	public ContainsStarts(final String line) {
		super(line.substring(TERM.length()));
		regex = Pattern.compile(String.format(REG_EX, Pattern.quote(getStart())));
	}

	@Override
	public boolean applies(final String url) {
		return regex.matcher(url).matches() && super.applies(url);
	}
}
