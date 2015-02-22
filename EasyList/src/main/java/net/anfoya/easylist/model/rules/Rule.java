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
	
	@Override
	public String toString() {
		return String.format("%s %s (%s) (%s)"
				, getClass().getSimpleName()
				, getParts()
				, getEffectiveLine()
				, getLine());
	}

	private String clean() {
		String line = this.line;
		if (line.contains("$")) {
			line = line.substring(0, line.indexOf("$"));
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

	private List<String> parse() {
		final List<String> parts = new ArrayList<String>();
		final String[] elements = effectiveLine.split("[\\^\\*]");
		if (elements.length > 0 && !elements[0].isEmpty()) {
			parts.addAll(Arrays.asList(elements));
		}
		
		return parts;
	}

	public boolean isEmpty() {
		return parts.isEmpty();
	}
}
