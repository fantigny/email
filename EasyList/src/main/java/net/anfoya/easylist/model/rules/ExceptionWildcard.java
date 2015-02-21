package net.anfoya.easylist.model.rules;


public class ExceptionWildcard extends ContainsWildcard {
	public static final String TERM = "@@||";

	public ExceptionWildcard(final String line) {
		super(line.substring("@@".length()));
	}
}
