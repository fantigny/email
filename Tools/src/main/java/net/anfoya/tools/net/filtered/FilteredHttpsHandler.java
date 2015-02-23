package net.anfoya.tools.net.filtered;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.tools.net.EmptyUrlConnection;
import net.anfoya.tools.net.filtered.engine.Matcher;
import net.anfoya.tools.net.filtered.engine.RuleSet;
import sun.net.www.protocol.https.Handler;

public class FilteredHttpsHandler extends Handler {
	private final Matcher matcher;

	public FilteredHttpsHandler(final RuleSet ruleSet) {
		this.matcher = new Matcher(ruleSet);
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		return matcher.matches(url.toString())
				? new EmptyUrlConnection()
				: super.openConnection(url);
	}
}
