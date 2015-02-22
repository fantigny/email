package net.anfoya.easylist.model.rules;


public class ExceptionHttpWildcard extends Exception {
	public static final String TERM = Exception.TERM + HttpWildcard.TERM;

	private final HttpWildcard wildcard;

	public ExceptionHttpWildcard(final String line) {
		super(line);
		wildcard = new HttpWildcard(line.substring(TERM.length()));
	}

	@Override
	public boolean applies(final String url) {
		return wildcard.applies(url) && super.applies(url);
	}
}
