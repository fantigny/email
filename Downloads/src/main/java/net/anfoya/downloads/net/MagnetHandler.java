package net.anfoya.downloads.net;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.tools.util.OperatingSystem;

public class MagnetHandler extends URLStreamHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MagnetHandler.class);

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		final String magnet = url.toString();
		LOGGER.info("starting: {}", magnet);
		try {
			switch(OperatingSystem.getInstance().getFamily()) {
			case MAC: {
		        final Process process = Runtime.getRuntime().exec(new String[] { "open", magnet } );
				final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line; while((line = br.readLine()) != null) LOGGER.debug(line);
				break;
			}
			case UNX: {
		        final Process process = Runtime.getRuntime().exec(new String[] { "xdg-open", magnet } );
				final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line; while((line = br.readLine()) != null) LOGGER.debug(line);
				break;
			}
			default:
				Desktop.getDesktop().open(new File(url.toURI()));
			}
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		}
		throw new UnsupportedOperationException("magnet link handled externally");
	}
}
