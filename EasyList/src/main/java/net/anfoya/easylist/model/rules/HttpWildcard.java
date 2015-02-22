package net.anfoya.easylist.model.rules;

import java.util.regex.Pattern;

public class HttpWildcard extends Rule {
	public static final String TERM = "||";
	private static final String REG_EX = "^https?:\\/\\/.*%s.*";
			
	private final Pattern regex;

	public HttpWildcard(final String line) {
		super(line.substring(TERM.length()));
		regex = Pattern.compile(String.format(REG_EX, Pattern.quote(getStart())));
	}

	@Override
	public boolean applies(final String url) {
		return regex.matcher(url).matches();
	}
}
