package net.anfoya.movies.javafx.consolidation;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Callback;
import jcifs.smb.SmbException;
import net.anfoya.cluster.LockManager;
import net.anfoya.movies.model.Movie;
import net.anfoya.movies.model.Tag;
import net.anfoya.movies.service.MovieFileService;
import net.anfoya.movies.service.MovieService;
import net.anfoya.movies.service.TagService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieConsolidationService extends ScheduledService<Boolean> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MovieConsolidationService.class);

	private final LockManager lockMgr;
	private final MovieService movieService;
	private final TagService tagService;
	private final MovieFileService fileService;

	private Callback<Set<Movie>, Void> toManyDeleteCallBack;

	public MovieConsolidationService(final LockManager lockMgr, final MovieService movieService, final TagService tagService, final MovieFileService fileService) {
		this.movieService = movieService;
		this.tagService = tagService;
		this.fileService = fileService;
		this.lockMgr = lockMgr;
	}

	public void setOnToManyDelete(final Callback<Set<Movie>, Void> callback) {
		toManyDeleteCallBack = callback;
	}

	@Override
	protected Task<Boolean> createTask() {
		return new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				if (lockMgr.lock()) {
					return consolidateMovieList();
				}
				return Boolean.FALSE;
			}
		};
	}

	@Override
	protected void succeeded() {
        super.succeeded();
        lockMgr.unlock();
    }

	@Override
	public void reset() {
		super.reset();
		lockMgr.unlock();
	}

	@Override
	protected void failed() {
		super.failed();
		lockMgr.unlock();
	};

    @Override
    protected void cancelled() {
        super.cancelled();
        lockMgr.unlock();
    }

	private Boolean consolidateMovieList() throws ConsolidationException {

		// get special tags
		final Tag toWatch = tagService.findOrCreate(Tag.TO_WATCH_NAME);

		// prepare hard drive movies map
		List<Movie> hdMovies;
		try {
			hdMovies = fileService.getList();
		} catch (SmbException | MalformedURLException e) {
			throw new ConsolidationException("error retrieving movies from network", e);
		}
		final Map<String, Movie> hdMovieMap = new HashMap<String, Movie>();
		for(final Movie movie: hdMovies) {
			hdMovieMap.put(movie.getPath(), movie);
		}

		// prepare database movies map
		final Set<Movie> dataSourceMovies = movieService.getAllMovies();
		final Map<String, Movie> dataSourceMovieMap = new HashMap<String, Movie>();
		for(final Movie movie: dataSourceMovies) {
			dataSourceMovieMap.put(movie.getPath(), movie);
		}

		// consolidate
		final Set<Movie> toAddMovies = new LinkedHashSet<Movie>();
		final Set<Movie> toDelMovies = new LinkedHashSet<Movie>(dataSourceMovieMap.values());
		final Set<Movie> lastModifiedUpdatedMovies = new LinkedHashSet<Movie>();
		final Set<Movie> noTagMovies = new LinkedHashSet<Movie>();
		for(final Entry<String, Movie> entry: hdMovieMap.entrySet()) {
			final String path = entry.getKey();
			final Movie hdMovie = entry.getValue();
			if (!dataSourceMovieMap.containsKey(path)) {
				// movie on hd but not in dataSource
				toAddMovies.add(hdMovie);
			} else {
				final Movie dataSourceMovie = dataSourceMovieMap.get(path);
				// use dataSource instance to get tag information
				hdMovieMap.put(path, dataSourceMovie);
				// movies remaining in this list will be deleted from dataSource
				toDelMovies.remove(dataSourceMovie);
				// movies with different modified date
				if (dataSourceMovie.getLastModified() != hdMovie.getLastModified()) {
					lastModifiedUpdatedMovies.add(dataSourceMovie.copyWithLastModified(hdMovie.getLastModified()));
				}
				// movies with no tag or only "To watch"
				if (dataSourceMovie.getTags().isEmpty() || dataSourceMovie.getTags().size() == 1 && dataSourceMovie.getTags().contains(toWatch)) {
					noTagMovies.add(dataSourceMovie);
				}
			}
		}

		LOGGER.info("to add: {}", toAddMovies.toString());
		LOGGER.info("to del: {}", toDelMovies.toString());
		LOGGER.info("no tag: {}", noTagMovies.toString());
		LOGGER.info("date updated: {}", lastModifiedUpdatedMovies.toString());

		boolean updated = false;

		// add movies
		if (!toAddMovies.isEmpty()) {
			movieService.addMovies(toAddMovies);
			updated = true;
		}

		// delete movies
		if (!toDelMovies.isEmpty()) {
			if (toDelMovies.size() > 5) {
				toManyDeleteCallBack.call(toDelMovies);
			} else {
				movieService.delMovies(toDelMovies);
				updated = true;
			}
		}

		// update last modified
		if (!lastModifiedUpdatedMovies.isEmpty()) {
			movieService.saveLastModified(lastModifiedUpdatedMovies);
			updated = true;
		}

		// add "No tag"
		if (!noTagMovies.isEmpty()) {
			final Tag noTag = tagService.findOrCreate(Tag.NO_TAG_NAME);
			tagService.addTagForMovies(noTag, noTagMovies);
			updated = true;
		}

		// clean tags
		final int count = tagService.delOrphanTags();
		updated |= count > 0;

		return Boolean.valueOf(updated);
	}
}
