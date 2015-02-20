package net.anfoya.downloads.javafx.net;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.net.www.protocol.http.Handler;

public class TorrentConnection extends GoBackUrlConnection {
	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentConnection.class);
	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") +"/";

	private final File file;
	
	protected TorrentConnection(URL url) {
		super(url);

		String filename = url.toString();
		filename = filename.substring(filename.lastIndexOf('/')+1, filename.length());
		filename = TEMP_DIR + filename;
		this.file = new File(filename);
	}

	@Override
	public void connect() throws IOException {
		LOGGER.info("downloading to {}", file);
		try {
			download();
		} catch (IOException e) {
			LOGGER.error("download {}", file,e);
			return;
		}
		
		LOGGER.info("starting {}", file);
		try {
			Desktop.getDesktop().open(file);
		} catch (IOException e) {
			LOGGER.error("starting {}", file,e);
			return;
		}
	}

	private void download() throws MalformedURLException, IOException {
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			InputStream in = new URL(null, url.toString(), new Handler()).openStream(); // avoid handler factory re-entrance
			bis = new BufferedInputStream(in);
			
			OutputStream out = new FileOutputStream(file);
			bos = new BufferedOutputStream(out);

			int data;
			while((data=bis.read()) != -1) {
				bos.write(data);
			}
			bos.flush();
		} finally {
			try {
				bis.close();
			} catch (final Exception e) {}
			try {
				bos.close();
			} catch (final Exception e) {}
		}
	}
}
