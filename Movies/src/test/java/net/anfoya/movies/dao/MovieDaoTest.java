package net.anfoya.movies.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.anfoya.movies.model.Config;
import net.anfoya.movies.model.Movie;
import net.anfoya.movies.model.Tag;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MovieDaoTest {
	private static final Set<Tag> EMPTY_TAG_SET = new LinkedHashSet<Tag>();

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

		Set<Movie> movies = movieDao.find(EMPTY_TAG_SET, rootName);
		Assert.assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { {
				add(new Movie(-1, rootName + "5", 55555, new LinkedHashSet<Tag>(), new HashMap<String, String>()));
				add(new Movie(rootName + "6", 666666));
		}});

		movies = movieDao.find(EMPTY_TAG_SET, rootName);
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

		final Set<Movie> movies = movieDao.find(EMPTY_TAG_SET, rootName);
		assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { {
			add(new Movie(rootName + 3, 333));
			add(new Movie(rootName + 4, 4444));
		}});

		final Set<Movie> addedMovies = movieDao.find(EMPTY_TAG_SET, rootName);
		assertEquals(2, addedMovies.size());

		movieDao.del(new LinkedHashSet<Movie>() { { add(addedMovies.iterator().next()); } });

		final Set<Movie> delMovies = movieDao.find(EMPTY_TAG_SET, addedMovies.iterator().next().getPath());
		assertEquals(0, delMovies.size());

		final Set<Movie> remainingMovies = movieDao.find(EMPTY_TAG_SET, rootName);
		assertEquals(1, remainingMovies.size());
	}

	@Test @SuppressWarnings("serial")
	public void updatePath() throws SQLException {
		final String path = "updatePath";

		Set<Movie> movies = movieDao.find(EMPTY_TAG_SET, path);
		Assert.assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(path, 0)); } });

		movies = movieDao.find(EMPTY_TAG_SET, path);
		Assert.assertEquals(1, movies.size());

		final String newPath = "newPath";
		movieDao.updatePath(movies.iterator().next().copyWithPath(newPath));

		movies = movieDao.find(EMPTY_TAG_SET, newPath);
		Assert.assertEquals(1, movies.size());
	}

	@Test @SuppressWarnings("serial")
	public void updateLastModified() throws SQLException {
		final String path = "updateLastModified";

		Set<Movie> movies = movieDao.find(EMPTY_TAG_SET, path);
		Assert.assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(path, 0)); } });

		movies = movieDao.find(EMPTY_TAG_SET, path);
		Assert.assertEquals(1, movies.size());

		final long newLastModified = 54321;
		movieDao.updateLastModified(movies.iterator().next().copyWithLastModified(newLastModified));

		movies = movieDao.find(EMPTY_TAG_SET, path);
		Assert.assertEquals(1, movies.size());
		Assert.assertEquals(54321, movies.iterator().next().getLastModified());
	}

	@SuppressWarnings("serial")
	@Test
	public void updateUrls() throws SQLException {
		final String path = "updateUrls";

		Set<Movie> movies = movieDao.find(EMPTY_TAG_SET, path);
		Assert.assertEquals(0, movies.size());

		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(path, 0)); } });

		movies = movieDao.find(EMPTY_TAG_SET, path);
		Assert.assertEquals(1, movies.size());
		Assert.assertTrue(movies.iterator().next().getUrlMap().isEmpty());

		final String name = new Config().getWebsites()[0].getName();
		final String url = "=url_Allocine";
		movieDao.updateUrls(movies.iterator().next().copyWithUrlMap(new HashMap<String, String>() {{ put(name, url); }}));

		movies = movieDao.find(EMPTY_TAG_SET, path);
		Assert.assertEquals(1, movies.size());
		Assert.assertTrue(movies.iterator().next().getUrlMap().containsKey(name));
		Assert.assertEquals(url, movies.iterator().next().getUrlMap().get(name));
	}

	@Test
	public void urlMapToString() {
		final Map<String, String> map = new HashMap<String, String>();
		map.put(new Config().getWebsites()[0].getName(), "=url_Allocine");
		map.put(new Config().getWebsites()[1].getName(), "=url_RotTom");
		map.put(new Config().getWebsites()[3].getName(), "=url_Google");

		final String urls = movieDao.urlMapToString(map);
		Assert.assertEquals("=url_Allocine~=url_RotTom~null~=url_Google~null~", urls);
	}

	@Test
	public void stringToUrlMap() {
		final Map<String, String> map = movieDao.stringToUrlMap("=url_Allocine~=url_RotTom~null~=url_Google~null~");

		Assert.assertTrue(map.containsKey(new Config().getWebsites()[0].getName()));
		Assert.assertEquals(map.get(new Config().getWebsites()[0].getName()), "=url_Allocine");
		Assert.assertTrue(map.containsKey(new Config().getWebsites()[1].getName()));
		Assert.assertEquals(map.get(new Config().getWebsites()[1].getName()), "=url_RotTom");
		Assert.assertTrue(map.containsKey(new Config().getWebsites()[3].getName()));
		Assert.assertEquals(map.get(new Config().getWebsites()[3].getName()), "=url_Google");
	}

	@Test
	public void emptyUrlMapToString() {
		final String urls = movieDao.urlMapToString(new HashMap<String, String>());
		assertEquals("", urls);
	}

	@Test
	public void emptyStringToUrlMap() {
		final Map<String, String> newMap = movieDao.stringToUrlMap("");
		assertTrue(newMap.isEmpty());
	}
}
