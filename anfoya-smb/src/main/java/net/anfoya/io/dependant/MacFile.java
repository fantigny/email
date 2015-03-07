package net.anfoya.io.dependant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import net.anfoya.io.AbstractSmbFile;
import net.anfoya.io.SmbFileExt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacFile extends AbstractSmbFile implements SmbFileExt {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacFile.class);

	private static String mountPoint = null;

	public MacFile(final URL url) {
		super(url);
	}

	public MacFile(final String url) throws MalformedURLException {
		super(url);
	}

	@Override
	public void showInFileExplorer() throws IOException {
		final String localPath = getLocalPath();
		LOGGER.info("opening file manager for: {}", localPath);
        final Process process = Runtime.getRuntime().exec(new String[] { "open", "-R", localPath } );
		final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line; while((line = br.readLine()) != null) LOGGER.debug(line);
	}

	@Override
	public void open() throws IOException {
		final String localPath = getLocalPath();
		LOGGER.info("starting: {}", localPath);
        final Process process = Runtime.getRuntime().exec(new String[] { "open", localPath } );
		final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line; while((line = br.readLine()) != null) LOGGER.debug(line);
	}

	private String getLocalPath() throws IOException {
		if (mountPoint == null) {
			mountPoint = getMountPoint();
			if (mountPoint == null) {
				mountPoint = System.getProperty("user.home") + "/smb_" + getShare();
				mountPoint = mountSmbShare(mountPoint);
			}
		}

		final String localPath = mountPoint + getFolder() + getName() + "";
		return localPath;
	}

	private String getMountPoint() throws IOException {
		LOGGER.info("resolving mount point for: {}", getShareUrl());

		String grepPattern = getURL().getHost() + "/" + getShare();
		grepPattern = grepPattern.substring(0, grepPattern.length() - 1);
		final Process process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "df -t smbfs| grep '" + grepPattern + "'| awk '{print $NF}'" });
        final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return br.readLine();
	}

	private String mountSmbShare(final String mountPoint) throws IOException {
		Process process;
		BufferedReader br;

		if (!new File(mountPoint).exists()) {
			LOGGER.info("creating folder: {}", mountPoint);
			process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "mkdir " + mountPoint });
			br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			try {
				String line; while((line = br.readLine()) != null) LOGGER.debug(line);
				if (process.waitFor() != 0) {
					return null;
				}
			} catch (final InterruptedException e) {
				LOGGER.error("creating folder: {}", mountPoint, e);
			}
		}

		LOGGER.info("mounting {} to {}", getShareUrl(), mountPoint);
		process = Runtime.getRuntime().exec(new String[] { "mount_smbfs", getShareUrl(), mountPoint });
		br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line; while((line = br.readLine()) != null) LOGGER.debug(line);
		try {
			if (process.waitFor() != 0) {
				return null;
			}
		} catch (final InterruptedException e) {
			LOGGER.error("mounting {} to {}", getShareUrl(), mountPoint, e);
		}

		return mountPoint;
	}
}
