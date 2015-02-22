package net.anfoya.easylist.model.rules;

public class ExceptionStarts extends ContainsStarts {
	public static final String TERM = Exception.TERM + ContainsStarts.TERM;

	public ExceptionStarts(String line) {
		super(line.substring(Exception.TERM.length()));
	}
}
