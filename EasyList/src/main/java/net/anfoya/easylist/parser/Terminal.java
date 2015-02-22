package net.anfoya.easylist.parser;

public enum Terminal {
	EXCEPTION("@@")
	, HTTP_WILDCARD("||")
	, STARTS("|")
	, ENDS("|")
	, COMMENT("!")
	, SECTION("[")
	, SEPARATOR("^")
	, WILDCARD("*")
	;

	private final String term;

	private Terminal(final String term) {
		this.term = term;
	}

	public String get() {
		return term;
	}

	public int length() {
		return term.length();
	}
}