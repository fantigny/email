package net.anfoya.movies.file;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// cifs://<user>:<pwd>@<host[:port]>/<share>/<fo/ld/er>/<name>.<ext>
public abstract class AbstractMovieFile extends SmbFile implements MovieFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMovieFile.class);
	private String shareUrl;
	private String share;
	private String folderUrl;
	private String folder;
	private String extension;
	private String shortPath;

	public AbstractMovieFile(final URL url) {
		super(url);
	}

	public AbstractMovieFile(final String url) throws MalformedURLException {
		super(url);
	}

	@Override
	public List<MovieFile> getListRec(final SmbFileFilter filter) throws SmbException, MalformedURLException {
		return getListRec(999, filter);
	}

	@Override
	public List<MovieFile> getListRec(final int depth, final SmbFileFilter filter) throws SmbException, MalformedURLException {
		return getListRec(getURL(), depth, filter, new ArrayList<MovieFile>());
	}

	private List<MovieFile> getListRec(final URL url, int depth, final SmbFileFilter filter, final List<MovieFile> allFiles) throws SmbException, MalformedURLException {
		LOGGER.info("exploring {}", url);

		for (final MovieFile file: MovieFileFactory.getFile(url).listSmbFiles(filter)) {
			if (file.isDirectory() && depth > 0) {
				getListRec(file.getURL(), depth--, filter, allFiles);
			} else {
				allFiles.add(file);
			}
		}

		return allFiles;
	}

	@Override
	public List<MovieFile> listSmbFiles(final SmbFileFilter smbFileFilter) throws SmbException {
		final SmbFile[] smbFiles = listFiles(smbFileFilter);
		final List<MovieFile> movieFiles = new ArrayList<MovieFile>();
		for(final SmbFile smbFile: smbFiles) {
			movieFiles.add(MovieFileFactory.getFile(smbFile.getURL()));
		}

		return movieFiles;
	}

	@Override
	public void renameTo(final MovieFile target) throws SmbException {
		renameTo(new SmbFile(target.getURL()));
	};

	@Override
	public long getLastModified() {
		return super.getLastModified() / 1000 * 1000;
	}

	// /fo/ld/er/name.ext
	@Override
	public String getShortPath() {
		if (shortPath == null) {
			final String share = getShare() + "/";
			shortPath = getURL().toString();
			shortPath = shortPath.substring(shortPath.lastIndexOf(share) + share.length());
			shortPath = "/" + shortPath;
		}

		return shortPath;
	}

	// ext
	@Override
	public String getExtension() {
		if (extension == null) {
			final String name = getName();
			if (name.contains(".")) {
				extension = name.substring(name.lastIndexOf('.') + 1);
			} else {
				extension = "";
			}
		}

		return extension;
	}

	// /fo/ld/er/
	@Override
	public String getFolder() {
		if (folder == null) {
			final String share = getShare() + "/";
			folder = getURL().toString();
			folder = folder.substring(folder.lastIndexOf(share) + share.length());
			if (folder.contains("/")) {
				folder = folder.substring(0, folder.lastIndexOf('/'));
			}
			folder = "/" + folder + "/";
		}

		return folder;
	}

	// cifs://user:pwd@host/share/fo/ld/er/
	@Override
	public String getFolderUrl() {
		if (folderUrl == null) {
			folderUrl = getURL().toString();
			if (folderUrl.contains("/")) {
				folderUrl = folderUrl.substring(0, folderUrl.lastIndexOf('/')) + "/";
			} else {
				folderUrl = "";
			}
		}

		return folderUrl;
	}

	// share
	@Override
	public String getShare() {
		if (share == null) {
			final String host = getURL().getHost();
			share = getURL().toString();
			share = share.substring(share.indexOf(host) + host.length() + 1);
			if (share.contains("/")) {
				share = share.substring(0, share.indexOf('/'));
			}
		}
		return share;
	}

	// cifs://user:pwd@host/share/
	@Override
	public String getShareUrl() {
		if (shareUrl == null) {
			final String share = getShare();
			shareUrl = getURL().toString();
			shareUrl = shareUrl.substring(0, shareUrl.indexOf(share) + share.length());
			shareUrl += "/";
		}
		return shareUrl;
	}
}
