package net.anfoya.java.net.filtered;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.java.net.EmptyUrlConnection;
import net.anfoya.java.net.filtered.engine.Matcher;
import net.anfoya.java.net.filtered.engine.RuleSet;
import sun.net.www.protocol.http.Handler;

public class FilteredHttpHandler extends Handler {
	private final Matcher matcher;

	public FilteredHttpHandler(final RuleSet ruleSet) {
		this.matcher = new Matcher(ruleSet);
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		return matcher.matches(url.toString())
				? new EmptyUrlConnection()
				: super.openConnection(url);
	}
}
