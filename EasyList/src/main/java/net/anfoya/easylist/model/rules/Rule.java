package net.anfoya.easylist.model.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Rule {
	private final String line;
	private final String effectiveLine;
	private final List<String> parts;

	public Rule(final String line) {
		this.line = line == null? "": line;
		this.effectiveLine = clean();
		this.parts = parse();
	}

	public boolean applies(final String url) {
		for(final String part: getParts()) {
			if (!url.contains(part)) {
				return false;
			}
		}
		return true;
	}

	public String getLine() {
		return line;
	}

	public String getEffectiveLine() {
		return effectiveLine;
	}

	public List<String> getParts() {
		return parts;
	}

	private String clean() {
		String line = this.line;
		if (line.contains("$")) {
			line = line.substring(0, line.indexOf("$"));
		}
		if (line.contains("~")) {
			line = line.substring(0, line.indexOf("~"));
		}

		return line;
	}

	private List<String> parse() {
		final List<String> urlParts = new ArrayList<String>();
		final String[] elements = line.split("[\\^\\*]");
		urlParts.addAll(Arrays.asList(elements));

		return urlParts;
	}
}
