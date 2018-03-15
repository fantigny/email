package net.anfoya.java.net.filtered.easylist.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLStreamHandler;

import net.anfoya.java.net.filtered.easylist.EasyListRuleSet;
import net.anfoya.java.net.filtered.easylist.model.Rule;
import net.anfoya.java.net.filtered.easylist.parser.Parser;
import net.anfoya.java.net.filtered.easylist.parser.ParserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternetLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(InternetLoader.class);

	private final URL url;

	public InternetLoader(final URL url) {
		this.url = url;
	}

	public EasyListRuleSet load() {
		LOGGER.info("loading {}", url);
		final long start = System.currentTimeMillis();
		EasyListRuleSet easyList;
		try {
			// avoid handler factory re-entrance
			@SuppressWarnings("restriction")
			final URLStreamHandler handler = "https".equals(url.getProtocol())
					? new sun.net.www.protocol.https.Handler()
					: new sun.net.www.protocol.http.Handler();
			final InputStream in = new URL(null, url.toString(), handler).openStream();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			easyList = new EasyListRuleSet(false);
			final Parser parser = new Parser();
			String line;
			while((line=reader.readLine()) != null) {
				try {
					final Rule rule = parser.parse(line);
					easyList.add(rule);
				} catch (final ParserException e) {
					LOGGER.error("parsing {}", line, e);
				}
			}
			LOGGER.info("loaded {} rules (in {}ms)", easyList.getRuleCount(), System.currentTimeMillis()-start);
		} catch (final IOException e) {
			LOGGER.error("reading {}", url, e);
			easyList = new EasyListRuleSet(false);
		}

		return easyList;
	}
}