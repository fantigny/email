package net.anfoya.easylist.model.rules;

import java.util.regex.Pattern;

public class Contains extends Rule {
	private static final String REGEX_SEP = "[^A-Za-z0-9_%.-]";

	private final Pattern regex;

	public Contains(final String line) {
		super("*" + line + "*");
		String regex = getEffectiveLine().replaceAll("\\^", REGEX_SEP);
		regex = regex.replaceAll("\\*", ".*");
		if (regex.isEmpty()) {
			regex = ".*";
		}
		this.regex = Pattern.compile(regex);
	}
	
	@Override
	public boolean applies(String url) {
		return regex.matcher(url).matches();
	}
}
