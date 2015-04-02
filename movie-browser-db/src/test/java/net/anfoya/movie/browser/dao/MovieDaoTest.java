package net.anfoya.movie.browser.dao;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import net.anfoya.movie.browser.dao.DataSource;
import net.anfoya.movie.browser.dao.MovieDao;
import net.anfoya.movie.browser.dao.MovieTagDao;
import net.anfoya.movie.browser.dao.TagDao;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Tag;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MovieDaoTest {
	private static DataSource dataSource;
	private static MovieDao movieDao;

	@BeforeClass
	public static void init() throws SQLException, IOException {
		dataSource = new DataSource();
		movieDao = new MovieDao(dataSource);
		movieDao.init();

		new TagDao(dataSource).init();
		new MovieTagDao(dataSource).init();
	}

	@AfterClass
	public static void shutdown() throws SQLException {
		dataSource.close();
	}

	@Test @SuppressWarnings("serial")
	public void add() throws SQLException {
		final String rootName = "add_";

		Set<Movie> movies = movieDao.find(rootName);
		Assert.assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { {
				add(new Movie(-1, rootName + "5", 55555, new LinkedHashSet<Tag>(), new HashMap<String, String>()));
				add(new Movie(rootName + "6", 666666));
		}});

		movies = movieDao.find(rootName);
		Assert.assertEquals(2, movies.size());

		int count = 0;
		for(final Movie movie: movies) {
			for(final Movie addedMovie: movies) {
				if (movie.getName().equals(addedMovie.getName())) {
					count++;
					assertEquals(movie.getLastModified(), addedMovie.getLastModified());
					assertEquals(movie.getPath(), addedMovie.getPath());
				}
			}
		}
		assertEquals(2, count);
	}

	@Test @SuppressWarnings("serial")
	public void del() throws SQLException {
		final String rootName = "del_";

		final Set<Movie> movies = movieDao.find(rootName);
		assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { {
			add(new Movie(rootName + 3, 333));
			add(new Movie(rootName + 4, 4444));
		}});

		final Set<Movie> addedMovies = movieDao.find(rootName);
		assertEquals(2, addedMovies.size());

		movieDao.del(new LinkedHashSet<Movie>() { { add(addedMovies.iterator().next()); } });

		final Set<Movie> delMovies = movieDao.find(addedMovies.iterator().next().getPath());
		assertEquals(0, delMovies.size());

		final Set<Movie> remainingMovies = movieDao.find(rootName);
		assertEquals(1, remainingMovies.size());
	}

	@Test @SuppressWarnings("serial")
	public void updatePath() throws SQLException {
		final String path = "updatePath";

		Set<Movie> movies = movieDao.find(path);
		Assert.assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(path, 0)); } });

		movies = movieDao.find(path);
		Assert.assertEquals(1, movies.size());

		final String newPath = "newPath";
		movieDao.updatePath(movies.iterator().next().copyWithPath(newPath));

		movies = movieDao.find(newPath);
		Assert.assertEquals(1, movies.size());
	}

	@Test @SuppressWarnings("serial")
	public void updateLastModified() throws SQLException {
		final String path = "updateLastModified";

		Set<Movie> movies = movieDao.find(path);
		Assert.assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(path, 0)); } });

		movies = movieDao.find(path);
		Assert.assertEquals(1, movies.size());

		final long newLastModified = 54321;
		movieDao.updateLastModified(movies.iterator().next().copyWithLastModified(newLastModified));

		movies = movieDao.find(path);
		Assert.assertEquals(1, movies.size());
		Assert.assertEquals(54321, movies.iterator().next().getLastModified());
	}

	@SuppressWarnings("serial")
	@Test
	public void updateUrls() throws SQLException {
		final String path = "updateUrls";

		Set<Movie> movies = movieDao.find(path);
		Assert.assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(path, 0)); } });

		movies = movieDao.find(path);
		Assert.assertEquals(1, movies.size());
		Assert.assertTrue(movies.iterator().next().getUrlMap().isEmpty());

		final String name = "allocine?";
		final String url = "=url_Allocine";
		movieDao.updateUrls(movies.iterator().next().copyWithUrlMap(new HashMap<String, String>() {{ put(name, url); }}));

		movies = movieDao.find(path);
		Assert.assertEquals(1, movies.size());
		Assert.assertTrue(movies.iterator().next().getUrlMap().containsKey(name));
		Assert.assertEquals(url, movies.iterator().next().getUrlMap().get(name));
	}
}
