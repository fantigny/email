package net.anfoya.movies.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Assert;
import net.anfoya.movies.model.Movie;
import net.anfoya.movies.model.Section;
import net.anfoya.movies.model.Tag;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MovieTagDaoTest {
	private static final Set<Tag> EMPTY = new HashSet<Tag>();

	private static DataSource dataSource;
	private static TagDao tagDao;
	private static MovieDao movieDao;
	private static MovieTagDao movieTagDao;

	@BeforeClass
	public static void init() throws SQLException, IOException {
		dataSource = new DataSource();

		tagDao = new TagDao(dataSource);
		tagDao.init();

		movieDao = new MovieDao(dataSource);
		movieDao.init();

		movieTagDao = new MovieTagDao(dataSource);
		movieTagDao.init();
	}

	@AfterClass
	public static void shutdown() throws SQLException {
		dataSource.close();
	}

	@Test @SuppressWarnings("serial")
	public void addTag() throws SQLException {
		final String name = "addTag";
		final String sectionName = name + "Section";

		tagDao.add(new Tag(name, sectionName));
		final Tag tag = tagDao.find(name);
		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(name, 0)); } });
		final Set<Movie> movies = movieDao.find(name);

		Assert.assertEquals(0, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));

		movieTagDao.addTag(movies, tag);

		Assert.assertEquals(1, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));
	}

	@Test @SuppressWarnings("serial")
	public void delTag() throws SQLException {
		final String name = "delTag";
		final String sectionName = name + "Section";

		tagDao.add(new Tag(name, sectionName));
		final Tag tag = tagDao.find(name);
		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(name, 0)); } });
		final Set<Movie> movies = movieDao.find(name);

		Assert.assertEquals(0, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));

		movieTagDao.addTag(movies, tag);

		Assert.assertEquals(1, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));

		movieTagDao.delTag(movies, tag);

		Assert.assertEquals(0, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));
	}

	@Test @SuppressWarnings("serial")
	public void del() throws SQLException {
		final String name = "delMovies";
		final String sectionName = name + "Section";

		tagDao.add(new Tag(name, sectionName));
		final Tag tag = tagDao.find(name);
		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(name, 0)); } });
		final Set<Movie> movies = movieDao.find(name);

		Assert.assertEquals(0, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));

		movieTagDao.addTag(movies, tag);

		Assert.assertEquals(1, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));

		movieTagDao.del(movies);

		Assert.assertEquals(0, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));
	}

	@Test @SuppressWarnings("serial")
	public void countSectionMovies() throws SQLException {
		final String name = "countSectionMovies";
		final String sectionName = name + "Section";

		tagDao.add(new Tag(name, sectionName));
		final Tag tag = tagDao.find(name);
		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(name, 0)); } });
		final Set<Movie> movies = movieDao.find(name);

		Assert.assertEquals(0, movieTagDao.countSectionMovies(new Section(sectionName), new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));

		movieTagDao.addTag(movies, tag);

		Assert.assertEquals(1, movieTagDao.countSectionMovies(new Section(sectionName), new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));
	}

	@Test @SuppressWarnings("serial")
	public void countMovies() throws SQLException {
		final String name = "countMovies";
		final String sectionName = name + "Section";

		tagDao.add(new Tag(name, sectionName));
		final Tag tag = tagDao.find(name);
		movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(name, 0)); } });
		final Set<Movie> movies = movieDao.find(name);

		Assert.assertEquals(0, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));

		movieTagDao.addTag(movies, tag);

		Assert.assertEquals(1, movieTagDao.countMovies(new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name));
	}
}
