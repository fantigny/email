package net.anfoya.java.net.download;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import net.anfoya.java.net.GoBackUrlConnection;
import net.anfoya.java.util.system.OperatingSystem;
import net.anfoya.java.util.system.OperatingSystem.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadLinkHandler extends URLStreamHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadLinkHandler.class);
	private static final Runtime RUNTIME = Runtime.getRuntime();
	private static final Desktop DESKTOP = Desktop.getDesktop();
	private static final Family OS_FAMILY = OperatingSystem.getInstance().getFamily();

	private static final String MAC_CMD = "open";
	private static final String UNX_CMD = "xdg-open";

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		final String link = url.toString();
		LOGGER.info("starting: {}", link);
		switch(OS_FAMILY) {
		case MAC: {
			LOGGER.info("starting: {} {}", MAC_CMD, link);
	        final Process process = RUNTIME.exec(new String[] { MAC_CMD, link } );
			final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line; while((line = br.readLine()) != null) {
				LOGGER.debug(line);
			}
			break;
		}
		case UNX: {
			LOGGER.info("starting: {} {}", UNX_CMD, link);
	        final Process process = RUNTIME.exec(new String[] { UNX_CMD, link } );
			final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line; while((line = br.readLine()) != null) {
				LOGGER.debug(line);
			}
			break;
		}
		default:
			try {
				LOGGER.info("Desktop.getDesktop().open(new File({}))", url.toURI());
				DESKTOP.open(new File(url.toURI()));
			} catch (final URISyntaxException e) {
				LOGGER.error("opening {}", url, e);
			}
		}

		return new GoBackUrlConnection("starting...");
	}
}
