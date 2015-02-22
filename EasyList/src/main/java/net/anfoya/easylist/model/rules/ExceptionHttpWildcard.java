package net.anfoya.easylist.model.rules;


public class ExceptionHttpWildcard extends ContainsHttpWildcard {
	public static final String TERM = "@@||";

	public ExceptionHttpWildcard(final String line) {
		super(line.substring("@@".length()));
	}
}
