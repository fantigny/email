package net.anfoya.easylist.model.rules;


public class Starts extends Rule {
	public static final String TERM = "|";

	private final String start;

	public Starts(final String line) {
		super(line.substring(TERM.length()));
		start = getParts().get(0);
	}

	@Override
	public boolean applies(final String url) {
		return url.startsWith(start);
	}
}
