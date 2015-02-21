package net.anfoya.easylist.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLStreamHandler;

import net.anfoya.easylist.model.EasyList;
import net.anfoya.easylist.model.rules.Rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Internet {
	private static final Logger LOGGER = LoggerFactory.getLogger(Internet.class);

	private final URL url;

	public Internet(final URL url) {
		this.url = url;
	}

	public EasyList load() {
		LOGGER.info("loading {}", url);
		EasyList easyList;
		try {
			// avoid handler factory re-entrance
			final URLStreamHandler handler = "https".equals(url.getProtocol())
					? new sun.net.www.protocol.https.Handler()
					: new sun.net.www.protocol.http.Handler();
			final InputStream in = new URL(null, url.toString(), handler).openStream();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			easyList = new EasyList();
			final Parser parser = new Parser();
			String line;
			while((line=reader.readLine()) != null) {
				LOGGER.debug("parsing {}", line);
				final Rule rule = parser.parse(line);
				LOGGER.debug("adding rule {}", rule);
				easyList.add(rule);
			}
			LOGGER.info("loaded {} filters", easyList.getRuleCount());
		} catch (final IOException e) {
			LOGGER.error("reading {}", url, e);
			easyList = new EasyList();
		}

		return easyList;
	}
}