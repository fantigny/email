package net.anfoya.java.net.filtered.easylist.model;

import java.io.Serializable;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class Rule implements Serializable {

	public static Rule getEmptyRule() {
		return new Rule(RuleType.empty, null, null, null);
	}

	private final RuleType type;
	private final Pattern regex;
	private final String effLine;
	private final String line;

	public Rule(final RuleType type, final Pattern regex, final String effLine, final String line) {
		this.type = type;
		this.regex = regex;
		this.effLine = effLine;
		this.line = line;
	}

	@Override
	public String toString() {
		return type.toString() + " \"" + effLine + "\" (" + regex.toString() + ")";
	}

	public boolean applies(final String url) {
		return regex.matcher(url).matches();
	}

	public Pattern getRegex() {
		return regex;
	}

	public String getEffectiveLine() {
		return effLine;
	}

	public String getLine() {
		return line;
	}

	public RuleType getType() {
		return type;
	}
}
