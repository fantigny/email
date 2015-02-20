package net.anfoya.movies.javafx;

import java.io.IOException;

import net.anfoya.movies.cluster.LockManager;
import net.anfoya.movies.cluster.StatusManager;
import net.anfoya.movies.cluster.UpdateManager;
import net.anfoya.movies.dao.DataSource;
import net.anfoya.movies.dao.MovieDao;
import net.anfoya.movies.dao.MovieTagDao;
import net.anfoya.movies.dao.TagDao;
import net.anfoya.movies.javafx.consolidation.FileConsolidationService;
import net.anfoya.movies.javafx.consolidation.MovieConsolidationService;
import net.anfoya.movies.javafx.movie.MoviePane;
import net.anfoya.movies.javafx.movielist.MovieListPane;
import net.anfoya.movies.javafx.taglist.SectionListPane;
import net.anfoya.movies.model.Profile;
import net.anfoya.movies.service.MovieFileService;
import net.anfoya.movies.service.MovieService;
import net.anfoya.movies.service.ProfileService;
import net.anfoya.movies.service.TagService;
import net.anfoya.tools.net.EasyListFilter;
import net.anfoya.tools.net.PersistentCookieStore;
import net.anfoya.tools.net.UrlFilter;

import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComponentBuilder.class);

	private final TagDao tagDao;
	private final MovieDao movieDao;
	private final MovieTagDao movieTagDao;

	private final Profile profile;
	private final TagService tagService;
	private final MovieService movieService;
	private final MovieFileService movieFileService;

	private final PersistentCookieStore cookieStore;
	private final UrlFilter urlFilter;

	private final FileConsolidationService fileConsoService;
	private final MovieConsolidationService movieConsoService;

	private final SectionListPane sectionListPane;
	private final MovieListPane movieListPane;
	private final MoviePane moviePane;

	private final StatusManager statusMgr;
	private final UpdateManager updateMgr;

	public ComponentBuilder() {
		DataSource dataSource = null;
		try {
			dataSource = new DataSource();
		} catch (final IOException e) {
			LOGGER.error("creating data source connection", e);
			System.exit(1);
		}

		Profile profile = null;
		try {
			profile = new ProfileService().getProfile();
		} catch (final IOException e) {
			LOGGER.error("creating profile", e);
			System.exit(2);
		}
		this.profile = profile;

		StatusManager statusMgr = null;
		try {
			statusMgr = new StatusManager("Movie cluster", new JChannel());
		} catch (final Exception e) {
			LOGGER.error("creating cluster", e);
			System.exit(3);
		}
		this.statusMgr = statusMgr;

		this.tagDao = new TagDao(dataSource);
		this.movieDao = new MovieDao(dataSource);
		this.movieTagDao = new MovieTagDao(dataSource);

		this.cookieStore = new PersistentCookieStore();
		this.urlFilter = new EasyListFilter();

		this.updateMgr = new UpdateManager(statusMgr);
		this.tagService = new TagService(updateMgr, tagDao, movieTagDao);
		this.movieFileService = new MovieFileService();
		this.movieService = new MovieService(tagService, movieFileService, updateMgr, movieDao);

		this.movieConsoService = new MovieConsolidationService(
				new LockManager("Movie consolidation", statusMgr)
				, movieService
				, tagService
				, movieFileService);

		this.fileConsoService = new FileConsolidationService(
				new LockManager("File consolidation", statusMgr)
				, movieService
				, movieFileService);

		this.sectionListPane = new SectionListPane(tagService);
		this.movieListPane = new MovieListPane(movieService);
		this.moviePane = new MoviePane(movieService, tagService, profile);
	}

	public SectionListPane buildSectionListPane() {
		return sectionListPane;
	}

	public MovieListPane buildMovieListPane() {
		return movieListPane;
	}

	public MoviePane buildMoviePane() {
		return moviePane;
	}

	public MovieConsolidationService buildMovieConsolidationService() {
		return movieConsoService;
	}

	public FileConsolidationService buildFileConsolidationService() {
		return fileConsoService;
	}

	public PersistentCookieStore buildCookieStore() {
		return cookieStore;
	}

	public MovieService buildMovieService() {
		return movieService;
	}

	public Profile buildProfile() {
		return profile;
	}

	public StatusManager buildStatusManager() {
		return statusMgr;
	}

	public UpdateManager buildUpdateManager() {
		return updateMgr;
	}

	public UrlFilter buildUrlFilter() {
		return urlFilter;
	}
}
