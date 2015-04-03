package net.anfoya.movie.browser.javafx.consolidation;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Callback;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import net.anfoya.cluster.LockManager;
import net.anfoya.io.SmbFileExt;
import net.anfoya.io.SmbFileExtFactory;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.service.MovieFileService;
import net.anfoya.movie.browser.service.MovieService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileConsolidationService extends Service<Void> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileConsolidationService.class);

	private final MovieService movieService;
	private final MovieFileService movieFileService;
	private final LockManager lockMgr;

	private final SmbFileFilter folderFilter = new SmbFileFilter() {
		@Override
		public boolean accept(final SmbFile file) throws SmbException {
			return file.getName().charAt(0) != '.' && file.isDirectory();
		}
	};

	private final SmbFileFilter noMovieFileFilter = new SmbFileFilter() {
		@Override
		public boolean accept(final SmbFile file) throws SmbException {
			if (file.getName().charAt(0) == '.') {
				return false;
			}
			if (file.isDirectory()) {
				return true;
			}
			final String name = file.getName().toLowerCase();
			for(final String ext: movieFileService.getMovieAndSubtitleExt()) {
				if (name.contains(ext)) {
					return false;
				}
			}
			return true;
		}
	};

	private Callback<Map<Movie, String>, Void> movieUpdateCallback;

	public FileConsolidationService(final LockManager fileConsoLock, final MovieService movieService, final MovieFileService movieFileService) {
		this.lockMgr = fileConsoLock;
		this.movieService = movieService;
		this.movieFileService = movieFileService;
	}

    @Override
	public void reset() {
        super.reset();
    	lockMgr.unlock();
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        lockMgr.unlock();
    }

	@Override
	protected void succeeded() {
        super.succeeded();
        lockMgr.unlock();
    }

	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {
			@Override
			protected Void call() throws ConsolidationException {
				if (lockMgr.lock()) {
					consolidateMovieFolders();
				}
				return null;
			}
		};
	}

	public void setOnMovieUpdated(final Callback<Map<Movie, String>, Void> callback) {
		this.movieUpdateCallback = callback;
	}

	private void consolidateMovieFolders() throws ConsolidationException {
		List<SmbFileExt> folders;
		try {
			final SmbFileExt movieFolder = SmbFileExtFactory.getFile(movieFileService.getSmbMovieUrl());
			folders = movieFolder.getListRec(0, folderFilter);
		} catch (SmbException | MalformedURLException e) {
			throw new ConsolidationException("getting folder list", e);
		}

		final Map<Movie, String> movieMap = new HashMap<Movie, String>();
		for(final SmbFileExt folder: folders) {
			final Set<Movie> movies = movieService.getMovies("/" + folder.getName());
			if (movies.size() == 1) {
				List<SmbFileExt> otherFiles;
				try {
					otherFiles = folder.listSmbFiles(noMovieFileFilter);
				} catch (final SmbException e) {
					throw new ConsolidationException("getting other file list", e);
				}
				movieMap.put(movies.iterator().next(), otherFiles.toString());
			}
		}

		LOGGER.info("to move: {}", movieMap.keySet());

		movieUpdateCallback.call(movieMap);

		return;
	}
}