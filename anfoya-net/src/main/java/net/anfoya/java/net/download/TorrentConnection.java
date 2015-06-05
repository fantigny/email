package net.anfoya.java.net.download;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.anfoya.java.net.GoBackUrlConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.net.www.protocol.http.Handler;

public class TorrentConnection extends GoBackUrlConnection {
	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentConnection.class);
	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") +"/";

	private final File file;

	protected TorrentConnection(final URL url) {
		super(url, "starting torrent...");

		final String[] urlParts = url.getPath().split("/");
		final String filename = urlParts[urlParts.length-1];

		this.file = new File(TEMP_DIR + filename);
	}

	@Override
	public void connect() throws IOException {
		final String path = file.getAbsolutePath();
		LOGGER.info("download torrent to {}", path);
		try {
			download();
		} catch (final IOException e) {
			LOGGER.error("downloading {}", path, e);
			return;
		}

		LOGGER.info("Desktop.getDesktop().open(\"{}\")", path);
		try {
			Desktop.getDesktop().open(file);
		} catch (final IOException e) {
			LOGGER.error("starting torrent on current system {}", path, e);
			return;
		}
	}

	private void download() throws MalformedURLException, IOException {
		try (	BufferedInputStream bis = new BufferedInputStream(new URL(null, url.toString(), new Handler()).openStream()); // avoid handler factory re-entrance
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				) {
			int data;
			while((data=bis.read()) != -1) {
				bos.write(data);
			}
			bos.flush();
		}
	}
}
