package net.anfoya.easylist.loader;

import net.anfoya.easylist.model.rules.Contains;
import net.anfoya.easylist.model.rules.ContainsStarts;
import net.anfoya.easylist.model.rules.ContainsHttpWildcard;
import net.anfoya.easylist.model.rules.EmptyRule;
import net.anfoya.easylist.model.rules.Exception;
import net.anfoya.easylist.model.rules.ExceptionStarts;
import net.anfoya.easylist.model.rules.ExceptionHttpWildcard;
import net.anfoya.easylist.model.rules.Rule;

public class Parser {
	private static final EmptyRule EMPTY_RULE = new EmptyRule();

	public Rule parse(final String line) {

		Rule rule;
		if (line.isEmpty()
				|| line.startsWith("!")
				|| line.startsWith("[")) {
			rule = EMPTY_RULE;
		} else if (line.startsWith(ExceptionHttpWildcard.TERM)) {
			rule = new ExceptionHttpWildcard(line);
		} else if (line.startsWith(ExceptionStarts.TERM)) {
			rule = new ExceptionStarts(line);
		} else if (line.startsWith(Exception.TERM)) {
			rule = new Exception(line);
		} else if (line.startsWith(ContainsHttpWildcard.TERM)) {
			rule = new ContainsHttpWildcard(line);
		} else if (line.startsWith(ContainsStarts.TERM)) {
			rule = new ContainsStarts(line);
		} else {
			rule = new Contains(line);
		}
		
		if (!(rule instanceof EmptyRule) && rule.isEmpty()) {
			rule = new EmptyRule();
		}

		return rule;
	}

}
