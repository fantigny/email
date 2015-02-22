package net.anfoya.easylist.parser;

public enum Regex {
	SEPARATOR("[^A-Za-z0-9_%.-]")
	, WILDCARD(".*")
	, HTTP_WILDCARD("^https?:\\/\\/")
	, STARTS("^")
	, ENDS("$")
	;

	private final String regexp;

	private Regex(final String regexp) {
		this.regexp = regexp;
	}

	public String get() {
		return regexp;
	}
}
