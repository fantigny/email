package net.anfoya.easylist.model.rules;


public abstract class Rule {
	private final String line;

	protected Rule() {
		this(null);
	}

	public Rule(final String line) {
		this.line = line == null? "": line;
	}

	public abstract boolean applies(final String url);

	public String getLine() {
		return line;
	}

	public String getStart() {
		String start = getLine();
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
		return String.format("%s \"%s\""
				, getClass().getSimpleName()
				, getLine());
	}

	public boolean isEmpty() {
		return getLine().isEmpty();
	}
}
