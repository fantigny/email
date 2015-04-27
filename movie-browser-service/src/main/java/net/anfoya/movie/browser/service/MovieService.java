package net.anfoya.movie.browser.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import jcifs.smb.SmbException;
import net.anfoya.cluster.UpdateManager;
import net.anfoya.io.SmbFileExt;
import net.anfoya.io.SmbFileExtFactory;
import net.anfoya.movie.browser.dao.MovieDao;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieService {
	private static final Logger LOGGER = LoggerFactory.getLogger(MovieService.class);

	private final MovieDao movieDao;

	private final TagService tagService;
	private final MovieFileService movieFileService;
	private final UpdateManager updateMgr;

	public MovieService(final TagService tagService, final MovieFileService movieFileService, final UpdateManager updateMgr, final MovieDao movieDao) {
		this.tagService = tagService;
		this.movieFileService = movieFileService;
		this.updateMgr = updateMgr;
		this.movieDao = movieDao;
	}

	public void saveMovieUrls(final Movie movie, final String websiteName, final String url) {
		try {
			final Map<String, String> urlMap = new HashMap<String, String>(movie.getUrlMap());
			urlMap.put(websiteName, url);
			movieDao.updateUrls(movie.copyWithUrlMap(urlMap));
			updateMgr.updatePerformed();
		} catch (final SQLException e) {
			LOGGER.error("saving page for movie: {}", movie.getName(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error saving page for movie: " + movie.getName());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}

	public void addMovies(final Set<Movie> movies) {
		final Set<Movie> added = new LinkedHashSet<Movie>();
		try {
			// add movies to dataSource
			movieDao.add(movies);
			updateMgr.updatePerformed();

			for(final Movie movie: movies) {
				added.add(movieDao.find(movie.getPath()).iterator().next());
			}
		} catch (final SQLException e) {
			LOGGER.error("adding movies: {}", movies.toString(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error adding movies: " + movies.toString());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}

		tagService.addTagForMovies(tagService.findOrCreate(Tag.NO_TAG_NAME), added);
		tagService.addTagForMovies(tagService.findOrCreate(Tag.TO_WATCH_NAME), added);
	}

	public void delMovies(final Set<Movie> movies) {
		// delete files
		for(final Movie movie: movies) {
			try {
				movieFileService.delete(movie);
			} catch (final IOException e) {
				LOGGER.error("deleting file: {}", movie.getPath(), e);
				final Alert alertDialog = new Alert(AlertType.ERROR);
				alertDialog.setHeaderText("error deleting file: " + movie.getPath());
				alertDialog.setContentText(e.getMessage());
				alertDialog.show();
			}
		}

		// delete tag relationships
		tagService.delTagsForMovies(movies);

		// delete movies
		try {
			movieDao.del(movies);
			updateMgr.updatePerformed();
		} catch (final SQLException e) {
			LOGGER.error("removing movies: {}", movies.toString(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error removing movies: " + movies.toString());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
		}
	}

	public void rename(final Movie movie, final String name) {
		Movie renamedMovie;
		try {
			renamedMovie = movieFileService.rename(movie, name);
		} catch (final IOException e) {
			LOGGER.error("renaming file for movie: {}", movie.getName(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error renaming file for movie: " + movie.getName());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}

		try {
			movieDao.updatePath(renamedMovie);
			updateMgr.updatePerformed();
		} catch (final SQLException e) {
			LOGGER.error("updating path for movie: {}", movie.getName(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error updating path for movie: " + movie.getName());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}

	public void play(final Movie movie) {
		try {
			movieFileService.playMovie(movie);
		} catch (URISyntaxException | IOException e) {
			LOGGER.error("playing movie: {}", movie.getName(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error playing movie: " + movie.getName());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}

	public void showInFileMngr(final Movie movie) {
		try {
			movieFileService.showInFileMngr(movie);
		} catch (IOException | URISyntaxException e) {
			LOGGER.error("showing file for: {}", movie.getName(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error showing file for: " + movie.getName());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}

	public Set<Movie> getAllMovies() {
		return getMovies("");
	}

	public Set<Movie> getMovies(final String namePattern) {
		final Set<Tag> emptySet = new LinkedHashSet<Tag>();
		return getMovies(emptySet, emptySet, emptySet, namePattern);
	}

	public Set<Movie> getMovies(final Set<Tag> tags, final Set<Tag> includes, final Set<Tag> excludes, final String namePattern) {
		try {
			return movieDao.find(tags, includes, excludes, namePattern);
		} catch (final SQLException e) {
			LOGGER.error("searching movies with name pattern: %{}% and tags: {}, exc {}", namePattern, includes, excludes, e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error searching movies with name pattern: %" + namePattern + "% and tags: " + includes.toString() + " exc: {}");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}
	}

	public void saveLastModified(final Set<Movie> lastModifiedUpdatedMovies) {
		try {
			movieDao.updateLastModified(lastModifiedUpdatedMovies);
			updateMgr.updatePerformed();
		} catch (final SQLException e) {
			LOGGER.error("updating date for movies: {}", lastModifiedUpdatedMovies.toString(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error updating date for movies: " + lastModifiedUpdatedMovies.toString());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}

	public void savePath(final Movie movedMovie) {
		try {
			movieDao.updatePath(movedMovie);
			updateMgr.updatePerformed();
		} catch (final SQLException e) {
			LOGGER.error("updating path for moved movie: {}", movedMovie.getName(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error updating path for moved movie: " + movedMovie.getName());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}

	public void consolidateFolder(final Movie movie) {
		LOGGER.info("moving {} to root folder", movie.getName());
		// move file
		Movie movedMovie;
		try {
			movedMovie = movieFileService.moveToRootFolder(movie);
		} catch (final IOException e) {
			//TODO: build a method/class to display errors/exceptions
			LOGGER.error("moving {} to root folder", movie.getName(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error moving movie: " + movie.getName() + " to root folder");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}

		// update dataSource with new path
		savePath(movedMovie);

		// delete old folder
		try {
			final SmbFileExt oldFile = movieFileService.getFile(movie);
			SmbFileExtFactory.getFile(oldFile.getFolderUrl()).delete();
		} catch (SmbException | MalformedURLException e) {
			LOGGER.error("deleting folder", e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error deleting folder");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}
}
