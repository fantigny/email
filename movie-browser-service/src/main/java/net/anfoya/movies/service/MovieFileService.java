package net.anfoya.movies.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import net.anfoya.io.SmbFileExt;
import net.anfoya.io.SmbFileExtFactory;
import net.anfoya.movies.model.Movie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieFileService {
	private static final Logger LOGGER = LoggerFactory.getLogger(MovieFileService.class);
	private static final String MOVIE_FILE_PROPERTY_FILENAME = "file.properties";

	private final String smbMovieUrl;
	private final String smbShareUrl;

	private final Set<String> movieExt;
	private final Set<String> subtitleExt;
	private final Set<String> movieAndSubtitleExt;

	private final SmbFileFilter movieFileFilter = new SmbFileFilter() {
		@Override
		public boolean accept(final SmbFile file) throws SmbException {
			final String filename = file.getName().toLowerCase();
			if (filename.charAt(0) == '.') {
				return false;
			}
			if (file.isDirectory()) {
				return true;
			}
			final String ext = filename.contains(".")? filename.substring(filename.lastIndexOf('.')): "";
			return movieExt.contains(ext);
		}
	};

	public MovieFileService() {
		String smbMovieUrl = null;
		String smbShareUrl = null;
		try {
			final Properties properties = new Properties();
			properties.load(getClass().getResourceAsStream(MOVIE_FILE_PROPERTY_FILENAME));

			smbMovieUrl = properties.getProperty("SMB_MOVIE_URL");
			if (!smbMovieUrl.isEmpty() && smbMovieUrl.charAt(smbMovieUrl.length() - 1) != '/') {
				smbMovieUrl += "/";
			}

			smbShareUrl = SmbFileExtFactory.getFile(smbMovieUrl).getShareUrl();
		} catch (final Exception e) {
			LOGGER.error("reading properties: {}", MOVIE_FILE_PROPERTY_FILENAME, e);
			System.exit(1);
		}
		this.smbMovieUrl = smbMovieUrl;
		this.smbShareUrl = smbShareUrl;


		final Set<String> movieExt = new LinkedHashSet<String>();
		final Set<String> subtitleExt = new LinkedHashSet<String>();;
		final Set<String> movieAndSubtitleExt = new LinkedHashSet<String>();
		try {
			final Properties properties = new Properties();
			properties.load(getClass().getResourceAsStream(MOVIE_FILE_PROPERTY_FILENAME));

			for(final String ext: ((String) properties.get("MOVIE_EXT")).split(",")) {
				movieExt.add("." + ext.trim().toLowerCase());
			}
			movieAndSubtitleExt.addAll(movieExt);
			for(final String ext: ((String) properties.get("SUBTITLE_EXT")).split(",")) {
				subtitleExt.add("." + ext.trim().toLowerCase());
				movieAndSubtitleExt.add("." + ext.trim().toLowerCase());
			}
		} catch (final IOException e) {
			LOGGER.error("loading property file: {}", MOVIE_FILE_PROPERTY_FILENAME);
			System.exit(3);
		}
		this.movieExt = movieExt;
		this.subtitleExt = subtitleExt;
		this.movieAndSubtitleExt = movieAndSubtitleExt;
	}

	private SmbFileFilter getSubtitleFileFilter(final Movie movie) {
		final String movieName = movie.getName().toLowerCase();
		return new SmbFileFilter() {
			@Override
			public boolean accept(final SmbFile file) throws SmbException {
				final String filename = file.getName();
				if (filename.charAt(0) == '.') {
					return false;
				}
				if (file.isDirectory()) {
					return false;
				}
				for(final String ext: subtitleExt) {
					if (filename.equalsIgnoreCase(movieName + ext)) {
						return true;
					}
				}
				return false;
			}
		};
	}

	public Movie moveToRootFolder(final Movie movie) throws IOException {
		// move movie file
		final SmbFileExt srcFile = getFile(movie);
		final SmbFileExt dstFile = SmbFileExtFactory.getFile(smbMovieUrl + srcFile.getName());
		final Movie dstMovie = movie.copyWithPath(dstFile.getShortPath());
		srcFile.renameTo(dstFile);

		// move subtitle file
		final SmbFileExt subFileFolder = SmbFileExtFactory.getFile(srcFile.getFolderUrl());
		final List<SmbFileExt> subSrcFiles = subFileFolder.listSmbFiles(getSubtitleFileFilter(movie));
		for(final SmbFileExt subSrcFile: subSrcFiles) {
			final SmbFileExt dstSubFile = SmbFileExtFactory.getFile(smbMovieUrl + subSrcFile.getName());
			subSrcFile.renameTo(dstSubFile);
		}

		return dstMovie;
	}

	public Movie rename(final Movie movie, final String name) throws IOException {
		// rename movie file
		final SmbFileExt srcFile = getFile(movie);
		final SmbFileExt dstFile = SmbFileExtFactory.getFile(srcFile.getFolderUrl() + name + "." + srcFile.getExtension());
		final Movie dstMovie = movie.copyWithPath(dstFile.getShortPath());
		srcFile.renameTo(dstFile);

		// move subtitle file
		final SmbFileExt subFileFolder = SmbFileExtFactory.getFile(srcFile.getFolderUrl());
		final List<SmbFileExt> subSrcFiles = subFileFolder.listSmbFiles(getSubtitleFileFilter(movie));
		for(final SmbFileExt subSrcFile: subSrcFiles) {
			final SmbFileExt dstSubFile = SmbFileExtFactory.getFile(subSrcFile.getFolderUrl() + name + "." + subSrcFile.getExtension());
			subSrcFile.renameTo(dstSubFile);
		}

		return dstMovie;
	}

	public void delete(final Movie movie) throws SmbException, MalformedURLException {
		final SmbFileExt movieFile = getFile(movie);
		if (movieFile.exists()) {
			movieFile.delete();

			final SmbFileExt subFileFolder = SmbFileExtFactory.getFile(movieFile.getFolderUrl());
			final List<SmbFileExt> subFiles = subFileFolder.listSmbFiles(getSubtitleFileFilter(movie));
			for(final SmbFileExt subFile: subFiles) {
				subFile.delete();
			}
		}
	}

	public List<Movie> getList() throws SmbException, MalformedURLException {
		final List<SmbFileExt> files = SmbFileExtFactory.getFile(smbMovieUrl).getListRec(movieFileFilter);
		final List<Movie> movies = new ArrayList<Movie>();
		for(final SmbFileExt file: files) {
			movies.add(new Movie(file.getShortPath(), file.getLastModified()));
		}

		return movies;
	}

	public void playMovie(final Movie movie) throws URISyntaxException, IOException {
		getFile(movie).open();
	}

	public void showInFileMngr(final Movie movie) throws IOException, URISyntaxException {
		getFile(movie).showInFileExplorer();
	}

	public Set<String> getMovieAndSubtitleExt() {
		return movieAndSubtitleExt;
	}

	public String getSmbMovieUrl() {
		return smbMovieUrl;
	}

	public SmbFileExt getFile(final Movie movie) throws MalformedURLException {
		String url = smbShareUrl;
		url = url.substring(0, url.length() - 1) + movie.getPath();
		return SmbFileExtFactory.getFile(url);
	}
}
