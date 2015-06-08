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

public class StartHandler extends URLStreamHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartHandler.class);

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		return openConnection(url.toString());
	}

	public URLConnection openConnection(final String address) throws IOException {
		LOGGER.info("starting: {}", address);
		switch(OperatingSystem.getInstance().getFamily()) {
		case MAC: {
	        final Process process = Runtime.getRuntime().exec(new String[] { "open", address } );
			final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line; while((line = br.readLine()) != null) {
				LOGGER.debug(line);
			}
			break;
		}
		case UNX: {
	        final Process process = Runtime.getRuntime().exec(new String[] { "xdg-open", address } );
			final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line; while((line = br.readLine()) != null) {
				LOGGER.debug(line);
			}
			break;
		}
		default:
			try {
				final URL url = new URL(address);
				Desktop.getDesktop().open(new File(url.toURI()));
			} catch (final URISyntaxException e) {
				LOGGER.error("opening {}", address, e);
			}
		}

		return new GoBackUrlConnection("starting torrent...");
	}
}