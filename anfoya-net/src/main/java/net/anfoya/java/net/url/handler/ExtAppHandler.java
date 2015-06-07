package net.anfoya.java.net.url.handler;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import net.anfoya.java.net.url.connection.GoBackUrlConnection;
import net.anfoya.java.util.system.OperatingSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtAppHandler extends URLStreamHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExtAppHandler.class);

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		final String magnet = url.toString();
		LOGGER.info("starting: {}", magnet);
		switch(OperatingSystem.getInstance().getFamily()) {
		case MAC: {
	        final Process process = Runtime.getRuntime().exec(new String[] { "open", magnet } );
			final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line; while((line = br.readLine()) != null) {
				LOGGER.debug(line);
			}
			break;
		}
		case UNX: {
	        final Process process = Runtime.getRuntime().exec(new String[] { "xdg-open", magnet } );
			final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line; while((line = br.readLine()) != null) {
				LOGGER.debug(line);
			}
			break;
		}
		default:
			try {
				Desktop.getDesktop().open(new File(url.toURI()));
			} catch (final URISyntaxException e) {
				LOGGER.error("opening {}", url, e);
			}
		}

		return new GoBackUrlConnection("starting torrent...");
	}
}
