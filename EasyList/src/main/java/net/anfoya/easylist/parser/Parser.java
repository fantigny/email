package net.anfoya.easylist.parser;

import java.util.regex.Pattern;

import net.anfoya.easylist.model.Rule;

public class Parser {
	public Rule parse(final String line) {
		return build(clean(line));
	}

	private String clean(String line) {
		if (line.isEmpty()
				|| line.startsWith(Terminal.COMMENT.get())
				|| line.startsWith(Terminal.SECTION.get())) {
			line = "";
		}
		if (line.contains("$")) {
			if (line.startsWith(Terminal.EXCEPTION.get())) {
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

	private Rule build(final String line) {
		if (line.isEmpty()) {
			return Rule.getEmptyRule();
		}

		String ruleStr = line;
		final boolean isException = ruleStr.startsWith(Terminal.EXCEPTION.get());
		if (isException) {
			ruleStr = ruleStr.substring(Terminal.EXCEPTION.length());
		}
		final boolean isHttpWildCard = ruleStr.startsWith(Terminal.HTTP_WILDCARD.get());
		if (isHttpWildCard) {
			ruleStr = ruleStr.substring(Terminal.HTTP_WILDCARD.length());
		}
		final boolean isStarts = ruleStr.startsWith(Terminal.STARTS.get());
		if (isStarts) {
			ruleStr = ruleStr.substring(Terminal.STARTS.length());
		}
		final boolean isEnds = ruleStr.startsWith(Terminal.ENDS.get());
		if (isEnds) {
			ruleStr = ruleStr.substring(Terminal.ENDS.length());
		}

		final String[] parts = line.split("[\\^\\*]");

		String regex = "";
		int lineIndex = 0;
		for(final String part: parts) {
			regex += part.isEmpty()? "": Pattern.quote(part);
			if (ruleStr.length() > lineIndex + part.length()) {
				switch (ruleStr.charAt(lineIndex + part.length())) {
				case '^':
					regex += Regex.SEPARATOR.get();
					lineIndex += part.length() + 1;
					break;
				case '*':
					regex += Regex.WILDCARD.get();
					lineIndex += part.length() + 1;
					break;
				}
			}
		}

		if (regex.isEmpty() && !ruleStr.isEmpty()) {
			regex = ruleStr
					.replaceAll("\\" + Terminal.SEPARATOR.get(), Regex.SEPARATOR.get())
					.replaceAll("\\" + Terminal.WILDCARD, Regex.WILDCARD.get());
		}

		if (isHttpWildCard) {
			regex = Regex.HTTP_WILDCARD.get() + regex;
		} else if (isStarts) {
			regex = Regex.STARTS.get() + regex;
		} else if (!regex.startsWith(Regex.STARTS.get())) {
			regex = Regex.WILDCARD.get() + regex;
		}

		if (isEnds) {
			regex += Regex.ENDS.get();
		} else if (!regex.endsWith(Regex.WILDCARD.get())) {
			regex += Regex.WILDCARD.get();
		}

		return new Rule(isException, Pattern.compile(regex), line);
	}
}
