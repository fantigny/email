package net.anfoya.java.net.url.connection;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadAndStartConnection extends GoBackUrlConnection {
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadAndStartConnection.class);
	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") +"/";

	private final File file;

	public DownloadAndStartConnection(final URL url) {
		super(url, "starting " + url.toString());

		final String[] urlParts = url.getPath().split("/");
		final String filename = urlParts[urlParts.length-1];

		this.file = new File(TEMP_DIR + filename);
	}

	@Override
	public void connect() throws IOException {
		LOGGER.info("downloading to {}", file);
		try {
			download();
		} catch (final IOException e) {
			LOGGER.error("download {}", file,e);
			return;
		}

		LOGGER.info("starting {}", file);
		try {
			Desktop.getDesktop().open(file);
		} catch (final IOException e) {
			LOGGER.error("starting {}", file,e);
			return;
		}
	}

	private void download() throws MalformedURLException, IOException {
		final URLStreamHandler handler;
		if ("http".equals(url.getProtocol())) {
			handler = new sun.net.www.protocol.http.Handler();
		} else if ("https".equals(url.getProtocol())) {
			handler = new sun.net.www.protocol.https.Handler();
		} else {
			return;
		}

		try (
				BufferedInputStream bis = new BufferedInputStream(new URL(null, url.toString(), handler).openStream()); // avoid handler factory re-entrance
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				) {
			int data; while((data=bis.read()) != -1) {
				bos.write(data);
			}
			bos.flush();
		}
	}
}
