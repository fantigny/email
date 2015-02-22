package net.anfoya.easylist.loader;

import java.util.regex.Pattern;

import net.anfoya.easylist.model.Rule2;

public class Parser {

	public Rule2 parse(final String line) {
		return buildRule(clean(line));
	}


	private String clean(String line) {
		if (line.isEmpty()
				|| line.startsWith("!")
				|| line.startsWith("[")) {
			return "";
		}
		if (line.contains("$")) {
			if (line.startsWith("@@")) {
				return "";
			} else {
				line = line.substring(0, line.indexOf("$"));
			}
		}
		if (line.contains("##") || line.contains("#@#")) { // todo: div selector
			line = "";
		}
		if (line.contains("~")) { // todo: opposite
			line = line.substring(0, line.indexOf("~"));
			if (line.length() == 1) {
				line = "";
			}
		}

		return line;
	}

	private static final Rule2 EMPTY_RULE = new Rule2();

	private static final String REGEX_SEP = "[^A-Za-z0-9_%.-]";
	private static final String REGEX_WIC = ".*";
	private static final String REGEX_HTTP_WIC = "^https?:\\/\\/";
	private static final String REGEX_STARTS = "^";
	private static final String REGEX_ENDS = "$";

	private static final String TERM_EXCEPTION = "@@";
	private static final String TERM_HTTP_WIC = "||";
	private static final String TERM_STARTS = "|";
	private static final String TERM_ENDS = "|";

	private Rule2 buildRule(final String line) {
		if (line.isEmpty()) {
			return EMPTY_RULE;
		}

		String ruleStr = line;
		final boolean isException = ruleStr.startsWith(TERM_EXCEPTION);
		if (isException) {
			ruleStr = ruleStr.substring(TERM_EXCEPTION.length());
		}
		final boolean isHttpWildCard = ruleStr.startsWith(TERM_HTTP_WIC);
		if (isHttpWildCard) {
			ruleStr = ruleStr.substring(TERM_HTTP_WIC.length());
		}
		final boolean isStarts = ruleStr.startsWith(TERM_STARTS);
		if (isStarts) {
			ruleStr = ruleStr.substring(TERM_STARTS.length());
		}
		final boolean isEnds = ruleStr.startsWith(TERM_ENDS);
		if (isEnds) {
			ruleStr = ruleStr.substring(TERM_ENDS.length());
		}

		final String[] parts = getParts(ruleStr);

		String regex = "";
		int lineIndex = 0;
		for(final String part: parts) {
			regex += part.isEmpty()? "": Pattern.quote(part);
			if (ruleStr.length() > lineIndex + part.length()) {
				switch (ruleStr.charAt(lineIndex + part.length())) {
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

		if (regex.isEmpty() && !ruleStr.isEmpty()) {
			regex = ruleStr.replaceAll("\\^", REGEX_SEP).replaceAll("\\*", REGEX_WIC);
		}

		if (isHttpWildCard) {
			regex = REGEX_HTTP_WIC + regex;
		} else if (isStarts) {
			regex = REGEX_STARTS + regex;
		} else if (!regex.startsWith(REGEX_WIC)) {
			regex = REGEX_WIC + regex;
		}

		if (isEnds) {
			regex += REGEX_ENDS;
		} else if (!regex.endsWith(REGEX_WIC)) {
			regex += REGEX_WIC;
		}

		return new Rule2(isException, Pattern.compile(regex), line);
	}

	private String[] getParts(final String line) {
		return line.split("[\\^\\*]");
	}
}
