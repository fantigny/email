package net.anfoya.easylist.model.rules;


public abstract class Rule {
	private final String line;
	private final String effectiveLine;

	protected Rule() {
		this(null);
	}
	
	public Rule(final String line) {
		this.line = line == null? "": line;
		this.effectiveLine = clean();
	}

	public abstract boolean applies(final String url);

	public String getLine() {
		return line;
	}

	public String getEffectiveLine() {
		return effectiveLine;
	}
	
	public String getStart() {
		String start = getEffectiveLine();
		if (start.contains("^")) {
			start = start.substring(0, start.indexOf("^"));
		}
		if (start.contains("*")) {
			start = start.substring(0, start.indexOf("*"));
		}
		return start;
	}
	
	@Override
	public String toString() {
		return String.format("%s (%s)"
				, getClass().getSimpleName()
				, getEffectiveLine());
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

	public boolean isEmpty() {
		return effectiveLine.isEmpty();
	}
}
