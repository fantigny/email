package net.anfoya.io.dependant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import net.anfoya.io.AbstractSmbFile;
import net.anfoya.io.SmbFileExt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnxFile extends AbstractSmbFile implements SmbFileExt {
	private static final Logger LOGGER = LoggerFactory.getLogger(UnxFile.class);
	private static final String MOUNT_POINT_PREFIX = System.getProperty("user.home") + "/smb_";

	private static String mountPoint = null;

	public UnxFile(final URL url) {
		super(url);
	}

	public UnxFile(final String url) throws MalformedURLException {
		super(url);
	}

	@Override
	public void showInFileExplorer() throws IOException {
		final String localPath = getLocalPath();
		LOGGER.info("opening file manager for: {}", localPath);
        final Process process = Runtime.getRuntime().exec(new String[] { "nautilus", localPath } );
		final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line; while((line = br.readLine()) != null) LOGGER.debug(line);
	}

	@Override
	public void open() throws IOException {
		final String localPath = getLocalPath();
		LOGGER.info("starting: {}", localPath);
        final Process process = Runtime.getRuntime().exec(new String[] { "xdg-open", localPath } );
		final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line; while((line = br.readLine()) != null) LOGGER.debug(line);
	}

	private String getLocalPath() throws IOException {
		if (mountPoint == null) {
			mountPoint = getMountPoint();
			if (mountPoint == null) {
				mountPoint = MOUNT_POINT_PREFIX + getShare();
				mountPoint = mountSmbShare(mountPoint);
			}
		}

		final String localPath = mountPoint + getFolder() + getName() + "";
		return localPath;
	}

	public String getMountPoint() throws IOException {
		LOGGER.info("resolving mount point for {}", getURL().toString());

		final String grepPattern = "/" + getShare();
		final Process process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "mount -t cifs| grep '" + grepPattern + "'| awk '{print $3}'" });
        final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return br.readLine();
	}

	public String mountSmbShare(final String mountPoint) throws IOException {
		// TODO: don't know how to do that...
		return mountPoint;
	}
}
