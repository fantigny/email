package net.anfoya.movie.browser.javafx;

import java.io.IOException;

import net.anfoya.cluster.LockManager;
import net.anfoya.cluster.StatusManager;
import net.anfoya.cluster.UpdateManager;
import net.anfoya.java.net.PersistentCookieStore;
import net.anfoya.java.net.filtered.easylist.EasyListRuleSet;
import net.anfoya.java.net.filtered.engine.RuleSet;
import net.anfoya.movie.browser.dao.DataSource;
import net.anfoya.movie.browser.dao.MovieDao;
import net.anfoya.movie.browser.dao.MovieTagDao;
import net.anfoya.movie.browser.dao.TagDao;
import net.anfoya.movie.browser.javafx.consolidation.FileConsolidationService;
import net.anfoya.movie.browser.javafx.consolidation.MovieConsolidationService;
import net.anfoya.movie.browser.javafx.movie.MoviePane;
import net.anfoya.movie.browser.javafx.movielist.MovieListPane;
import net.anfoya.movie.browser.javafx.taglist.SectionListPane;
import net.anfoya.movie.browser.model.Profile;
import net.anfoya.movie.browser.service.MovieFileService;
import net.anfoya.movie.browser.service.MovieService;
import net.anfoya.movie.browser.service.ProfileService;
import net.anfoya.movie.browser.service.TagService;

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
	private final RuleSet urlFilter;

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
		this.urlFilter = new EasyListRuleSet(false);

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

	public RuleSet buildUrlFilter() {
		return urlFilter;
	}
}
