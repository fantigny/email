package net.anfoya.easylist.model;

import java.util.regex.Pattern;

public class Rule2 {

	private final boolean exception;
	private final Pattern regex;
	private final String line;

	public Rule2() {
		this(false, null, null);
	}

	public Rule2(final boolean exception, final Pattern regex, final String line) {
		this.exception = exception;
		this.regex = regex;
		this.line = line;
	}

	public boolean isException() {
		return exception;
	}

	public boolean applies(final String url) {
		return regex.matcher(url).matches();
	}

	public boolean isEmpty() {
		return regex == null;
	}

	@Override
	public String toString() {
		return exception? "exception": "exclusion" + " " + line;
	}
}
