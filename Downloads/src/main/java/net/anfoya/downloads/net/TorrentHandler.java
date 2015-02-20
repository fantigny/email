package net.anfoya.downloads.net;

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

public class TorrentHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentHandler.class);
	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") +"/";

	public void launch(final URL url) {
		LOGGER.info("handling {}", url);
		File file = getTempFilename(url);
		
		LOGGER.info("downloading to {}", file);
		try {
			download(url, file);
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
	
	private File getTempFilename(URL url) {
		String filename = url.toString();
		filename = filename.substring(filename.lastIndexOf('/')+1, filename.length());
		filename = TEMP_DIR + filename;
		return new File(filename);
	}

	private void download(URL fromUrl, File toFile) throws MalformedURLException, IOException {
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			InputStream in = new URL(null, fromUrl.toString(), new Handler()).openStream(); // avoid handler factory re-entrance
			bis = new BufferedInputStream(in);
			
			OutputStream out = new FileOutputStream(toFile);
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
