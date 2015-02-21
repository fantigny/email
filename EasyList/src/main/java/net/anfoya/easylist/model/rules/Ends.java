package net.anfoya.easylist.model.rules;


public class Ends extends Rule {
	public static final String TERM = "|";

	private final String end;

	public Ends(final String line) {
		super(null);
		end = getParts().get(getParts().size()-1);
	}

	@Override
	public boolean applies(final String url) {
		return url.endsWith(end);
	}
}
