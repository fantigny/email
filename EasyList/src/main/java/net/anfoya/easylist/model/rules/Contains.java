package net.anfoya.easylist.model.rules;

import java.util.regex.Pattern;

public class Contains extends Rule {
	private static final String REGEX_SEP = "[^A-Za-z0-9_%.-]";
	private static final String REGEX_WIC = ".*";

	private final Pattern regex;

	public Contains(final String line) {
		super(line);

		String regex = "";
		int lineIndex = 0;
		final String[] parts = line.split("[\\^\\*]");
		for(final String part: parts) {
			regex += part.isEmpty()? "": Pattern.quote(part);
			if (line.length() > lineIndex + part.length()) {
				switch (line.charAt(lineIndex + part.length())) {
				case '^':
					regex += REGEX_SEP;
					lineIndex += part.length() + 1;
					break;
				case '*':
					regex += REGEX_WIC;
					lineIndex += part.length() + 1;
					break;
				}
			}
		}
		if (regex.isEmpty() && !line.isEmpty()) {
			regex = line.replaceAll("\\^", REGEX_SEP).replaceAll("\\*", REGEX_WIC);
		}
		if (!regex.startsWith(REGEX_WIC)) {
			regex = REGEX_WIC + regex;
		}
		if (!regex.endsWith(REGEX_WIC)) {
			regex += REGEX_WIC;
		}

		this.regex = Pattern.compile(regex);
	}

	@Override
	public boolean applies(final String url) {
		return regex.matcher(url).matches();
	}
}
