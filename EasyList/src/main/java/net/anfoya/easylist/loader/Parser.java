package net.anfoya.easylist.loader;

import net.anfoya.easylist.model.rules.Contains;
import net.anfoya.easylist.model.rules.ContainsWildcard;
import net.anfoya.easylist.model.rules.EmptyRule;
import net.anfoya.easylist.model.rules.Exception;
import net.anfoya.easylist.model.rules.ExceptionWildcard;
import net.anfoya.easylist.model.rules.Rule;

public class Parser {
	private static final EmptyRule EMPTY_RULE = new EmptyRule();

	public Rule parse(final String line) {

		Rule rule;
		if (line.isEmpty()
				|| line.startsWith("!")
				|| line.startsWith("[")) {
			rule = EMPTY_RULE;
		} else if (line.startsWith(ExceptionWildcard.TERM)) {
			rule = new ExceptionWildcard(line);
		} else if (line.startsWith(Exception.TERM)) {
			rule = new Exception(line);
		} else if (line.startsWith(ContainsWildcard.TERM)) {
			rule = new ContainsWildcard(line);
		} else {
			rule = new Contains(line);
		}
		
		if (!(rule instanceof EmptyRule) && rule.isEmpty()) {
			rule = new EmptyRule();
		}

		return rule;
	}

}
