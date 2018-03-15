package net.anfoya.java.net.filtered.easylist.parser;

import java.util.regex.Pattern;

import net.anfoya.java.net.filtered.easylist.model.Rule;
import net.anfoya.java.net.filtered.easylist.model.RuleType;

public class Parser {
	public Rule parse(final String line) throws ParserException {
		// get effective line for parsing
		String effLine;
		try {
			effLine = clean(line);
		} catch(final Exception e) {
			throw new ParserException("cleaning " + line, e);
		}
		if (effLine.isEmpty()) {
			return Rule.getEmptyRule();
		}

		// check if regular or exception
		final RuleType type;
		if (effLine.startsWith(Terminal.EXCEPTION.value())) {
			type = RuleType.exception;
			effLine = effLine.substring(Terminal.EXCEPTION.length());
		} else {
			type = RuleType.exclusion;
		}

		// transform effective line to regular expression
		Pattern regex;
		try {
			regex = buildRegex(effLine);
		} catch(final Exception e) {
			throw new ParserException("building regex for " + effLine, e);
		}

		return new Rule(type, regex, effLine, line);
	}

	private String clean(String line) {
		if (line.isEmpty()
				|| line.startsWith(Terminal.COMMENT.value())
				|| line.startsWith(Terminal.SECTION.value())) {
			line = "";
		}
		if (line.contains("$")) {
			if (line.startsWith(Terminal.EXCEPTION.value())) {
				line = "";
			} else {
				line = line.substring(0, line.indexOf("$"));
			}
		}
		if (line.startsWith(Terminal.DIV.value())) {
			if (line.contains("href")
						&& line.contains("=\"")
						&& line.contains("\"]")) {
				line = line.substring(line.indexOf("=\"") + 2);
				line = line.substring(0, line.indexOf("\"]"));
			} else {
				line = "";
			}
		}
		if (line.startsWith("#@#")) { // todo: div selector exception
			line = "";
		}

		return line.trim();
	}

	private Pattern buildRegex(final String line) {
		String rule = line;
		int ruleIndex = 0;

		// check beginning and end
		final boolean isHttpWildCard = rule.startsWith(Terminal.HTTP_WILDCARD.value());
		if (isHttpWildCard) {
			rule = rule.substring(Terminal.HTTP_WILDCARD.length());
		}
		final boolean isStarts = rule.startsWith(Terminal.STARTS.value());
		if (isStarts) {
			rule = rule.substring(Terminal.STARTS.length());
		}
		final boolean isEnds = rule.startsWith(Terminal.ENDS.value());
		if (isEnds) {
			rule = rule.substring(Terminal.ENDS.length());
		}

		// get wildcard parts
		final String[] parts = line.split(Regex.SPLIT_RULE.value());

		// build regex
		String regex = "";
		for(final String part: parts) {
			regex += part.isEmpty()? "": Pattern.quote(part);
			if (rule.length() > ruleIndex + part.length()) {
				switch (rule.charAt(ruleIndex + part.length())) {
				case '^':
					regex += Regex.SEPARATOR.value();
					ruleIndex += part.length() + 1;
					break;
				case '*':
					regex += Regex.WILDCARD.value();
					ruleIndex += part.length() + 1;
					break;
				}
			}
		}

		if (regex.isEmpty() && !rule.isEmpty()) {
			regex = rule
					.replaceAll("\\" + Terminal.SEPARATOR.value(), Regex.SEPARATOR.value())
					.replaceAll("\\" + Terminal.WILDCARD.value(), Regex.WILDCARD.value());
		}

		if (isHttpWildCard) {
			regex = Regex.HTTP_WILDCARD.value() + regex;
		} else if (isStarts) {
			regex = Regex.STARTS.value() + regex;
		} else if (!regex.startsWith(Regex.STARTS.value())) {
			regex = Regex.WILDCARD.value() + regex;
		}

		if (isEnds) {
			regex += Regex.ENDS.value();
		} else if (!regex.endsWith(Regex.WILDCARD.value())) {
			regex += Regex.WILDCARD.value();
		}

		return Pattern.compile(regex);
	}
}
