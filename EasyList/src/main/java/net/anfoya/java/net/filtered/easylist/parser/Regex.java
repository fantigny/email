package net.anfoya.java.net.filtered.easylist.parser;

public enum Regex {
	SEPARATOR("[^A-Za-z0-9_%.-]")
	, WILDCARD(".*")
	, HTTP_WILDCARD("^https?:\\/\\/")
	, STARTS("^")
	, ENDS("$")

	, SPLIT("[\\^\\*]")
	;

	private final String value;

	private Regex(final String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
