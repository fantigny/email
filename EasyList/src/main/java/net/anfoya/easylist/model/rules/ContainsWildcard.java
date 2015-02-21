package net.anfoya.easylist.model.rules;


public class ContainsWildcard extends Contains {
	public static final String TERM = "||";

	private final StartsWildcard wildcard;

	public ContainsWildcard(final String line) {
		super(line.substring(TERM.length()));
		wildcard = new StartsWildcard(line);
	}

	@Override
	public boolean applies(final String url) {
		return wildcard.applies(url) && super.applies(url);
	}
}
