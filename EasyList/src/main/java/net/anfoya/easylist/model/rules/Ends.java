package net.anfoya.easylist.model.rules;

import java.util.regex.Pattern;


public class Ends extends Rule {
	public static final String TERM = "|";
	private static final String REG_EX = ".*%s";

	private final Pattern regex;

	public Ends(final String line) {
		super(line);
		regex = Pattern.compile(String.format(REG_EX, getEnds()));
	}

	@Override
	public boolean applies(final String url) {
		return regex.matcher(url).matches();
	}
}
