package net.anfoya.java.net.url.filter;

public class Matcher {
	private final RuleSet ruleSet;

	public Matcher(final RuleSet ruleSet) {
		this.ruleSet = ruleSet;
	}

	public boolean matches(final String url) {
		if (ruleSet.isWithException() && ruleSet.matchesException(url)) {
			// URL matches an exception rule
			return false;
		} else if (ruleSet.matchesExclusion(url)) {
			// URL matches an exclusion rule
			return true;
		} else {
			// no rule applies
			return false;
		}
	}
}
