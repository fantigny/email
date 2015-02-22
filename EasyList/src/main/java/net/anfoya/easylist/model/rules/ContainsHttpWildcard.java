package net.anfoya.easylist.model.rules;


public class ContainsHttpWildcard extends Contains {
	public static final String TERM = "||";

	private final HttpWildcard wildcard;

	public ContainsHttpWildcard(final String line) {
		super(line.substring(TERM.length()));
		wildcard = new HttpWildcard(line);
	}

	@Override
	public boolean applies(final String url) {
		return wildcard.applies(url) && super.applies(url);
	}
}
