package net.anfoya.movies.dao.javafx.movie;

import java.util.LinkedHashSet;

import net.anfoya.cluster.StatusManager;
import net.anfoya.cluster.UpdateManager;
import net.anfoya.movies.dao.DataSource;
import net.anfoya.movies.dao.MovieDao;
import net.anfoya.movies.dao.MovieTagDao;
import net.anfoya.movies.dao.TagDao;
import net.anfoya.movies.javafx.movie.MoviePane;
import net.anfoya.movies.model.Movie;
import net.anfoya.movies.model.Profile;
import net.anfoya.movies.service.MovieFileService;
import net.anfoya.movies.service.MovieService;
import net.anfoya.movies.service.ProfileService;
import net.anfoya.movies.service.TagService;

import org.jgroups.JChannel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.saxsys.javafx.test.JfxRunner;
import de.saxsys.javafx.test.TestInJfxThread;

@RunWith(JfxRunner.class)
public class MoviePaneTest {

	private static Profile profile;
	private static DataSource dataSource;
	private static TagDao tagDao;
	private static MovieDao movieDao;
	private static MovieTagDao movieTagDao;
	private static MovieFileService movieFileService;
	private static TagService tagService;
	private static MovieService movieService;

	@BeforeClass
	public static void init() throws Exception {
		profile = new ProfileService().getProfile();
		dataSource = new DataSource();
		tagDao = new TagDao(dataSource);
		tagDao.init();
		movieDao = new MovieDao(dataSource);
		movieDao.init();
		movieTagDao = new MovieTagDao(dataSource);
		movieTagDao.init();

		final UpdateManager updateMgr = new UpdateManager(new StatusManager("Movie pane test", new JChannel()));

		movieFileService = new MovieFileService();

		tagService = new TagService(updateMgr, tagDao , movieTagDao);
		movieService = new MovieService(tagService, movieFileService, updateMgr, movieDao);
	}

	@Test @TestInJfxThread @SuppressWarnings("serial")
	public void create() {
		final MoviePane moviePane = new MoviePane(movieService, tagService, profile);

		moviePane.load(new LinkedHashSet<Movie>() { { add(new Movie("my movie.avi", 1L)); } });
	}
}
