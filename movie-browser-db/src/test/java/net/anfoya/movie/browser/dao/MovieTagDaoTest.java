package net.anfoya.movie.browser.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Section;
import net.anfoya.movie.browser.model.Tag;

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

		Assert.assertEquals(0, movieTagDao.countSectionMovies(new Section(sectionName), new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name, ""));

		movieTagDao.addTag(movies, tag);

		Assert.assertEquals(1, movieTagDao.countSectionMovies(new Section(sectionName), new LinkedHashSet<Tag>() { { add(tag); } }, EMPTY, name, ""));
	}

	@Test @SuppressWarnings("serial")
	public void countMoviesByName() throws SQLException {
		final String name = "countMoviesByName";
		final String sectionName = "section" + name;

		Assert.assertEquals(0, movieTagDao.countMovies(EMPTY, EMPTY, name));

		Tag aTag = new Tag(name + "_aTag", sectionName);
		tagDao.add(aTag);
		aTag = tagDao.find(aTag.getName());
		for(int i=0; i<3; i++) {
			final String movieName = name + i;
			movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(movieName, 0)); } });
			movieTagDao.addTag(movieDao.find(movieName), aTag);
		}

		Assert.assertEquals(3, movieTagDao.countMovies(EMPTY, EMPTY, name));
	}

	@Test @SuppressWarnings("serial")
	public void countMoviesIncluding() throws SQLException {
		final String name = "countMoviesIncluding";
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
	public void countMoviesExcluding() throws SQLException {
		final String name = "countMoviesExcluding";
		final String sectionName = name + "Section";

		final List<Tag> tags = new ArrayList<Tag>();
		for(int i=0; i<3; i++) {
			final String tagName = name + i;
			tagDao.add(new Tag(tagName, sectionName));
			tags.add(tagDao.find(tagName));
		}

		final Tag tag1 = tags.get(0);
		final Tag tag2 = tags.get(1);
		final Tag tag3 = tags.get(2);
		final Set<Tag> setTag1 = new HashSet<Tag>() { { add(tag1); } };
		final Set<Tag> setTag2 = new HashSet<Tag>() { { add(tag2); } };
		final Set<Tag> setTag3 = new HashSet<Tag>() { { add(tag3); } };
		final Set<Tag> setTag12 = new HashSet<Tag>() { { add(tag1); add(tag2); } };
		final Set<Tag> setTag13 = new HashSet<Tag>() { { add(tag1); add(tag3); } };
		final Set<Tag> setTag23 = new HashSet<Tag>() { { add(tag2); add(tag3); } };
		final Set<Tag> setTag123 = new HashSet<Tag>() { { add(tag1); add(tag2); add(tag3); } };

		/*
		 * no movies
		 */

		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, EMPTY, name));		// in tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, EMPTY, name));		// in tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, EMPTY, name));		// in tag3
		Assert.assertEquals(0, movieTagDao.countMovies(setTag12, EMPTY, name));		// in tag1, tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag13, EMPTY, name));		// in tag1, tag3
		Assert.assertEquals(0, movieTagDao.countMovies(setTag23, EMPTY, name));		// in tag2, tag3
		Assert.assertEquals(0, movieTagDao.countMovies(setTag123, EMPTY, name));	// in tag1, tag2, tag3

		Assert.assertEquals(0, movieTagDao.countMovies(EMPTY, setTag1, name));		// ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(EMPTY, setTag2, name));		// ex tag2
		Assert.assertEquals(0, movieTagDao.countMovies(EMPTY, setTag3, name));		// ex tag3
		Assert.assertEquals(0, movieTagDao.countMovies(EMPTY, setTag12, name));		// ex tag1, tag2
		Assert.assertEquals(0, movieTagDao.countMovies(EMPTY, setTag23, name));		// ex tag2, tag3
		Assert.assertEquals(0, movieTagDao.countMovies(EMPTY, setTag123, name));	// ex tag1, tag2, tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, setTag1, name));	// in tag1 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, setTag2, name));	// in tag1 ex tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, setTag3, name));	// in tag1 ex tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, setTag1, name));	// in tag2 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, setTag2, name));	// in tag2 ex tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, setTag3, name));	// in tag2 ex tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag1, name));	// in tag3 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag2, name));	// in tag3 ex tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag3, name));	// in tag3 ex tag3

		/*
		 * with 3 movies
		 */

		Tag aTag = new Tag(name + "_aTag", sectionName);
		tagDao.add(aTag);
		aTag = tagDao.find(aTag.getName());
		for(int i=0; i<3; i++) {
			final String movieName = name + i;
			movieDao.add(new HashSet<Movie>() { { add(new Movie(movieName, 0)); } });
			movieTagDao.addTag(movieDao.find(movieName), aTag);
		}
		final List<Movie> movies = new ArrayList<Movie>(movieDao.find(name));

		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, EMPTY, name));		// in tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, EMPTY, name));		// in tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, EMPTY, name));		// in tag3
		Assert.assertEquals(0, movieTagDao.countMovies(setTag12, EMPTY, name));		// in tag1, tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag13, EMPTY, name));		// in tag1, tag3
		Assert.assertEquals(0, movieTagDao.countMovies(setTag23, EMPTY, name));		// in tag2, tag3
		Assert.assertEquals(0, movieTagDao.countMovies(setTag123, EMPTY, name));	// in tag1, tag2, tag3

		Assert.assertEquals(3, movieTagDao.countMovies(EMPTY, setTag1, name));		// ex tag1
		Assert.assertEquals(3, movieTagDao.countMovies(EMPTY, setTag2, name));		// ex tag2
		Assert.assertEquals(3, movieTagDao.countMovies(EMPTY, setTag3, name));		// ex tag3
		Assert.assertEquals(3, movieTagDao.countMovies(EMPTY, setTag12, name));		// ex tag1, tag2
		Assert.assertEquals(3, movieTagDao.countMovies(EMPTY, setTag13, name));		// ex tag1, tag3
		Assert.assertEquals(3, movieTagDao.countMovies(EMPTY, setTag23, name));		// ex tag2, tag3
		Assert.assertEquals(3, movieTagDao.countMovies(EMPTY, setTag123, name));	// ex tag1, tag2, tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, setTag1, name));	// in tag1 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, setTag2, name));	// in tag1 ex tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, setTag3, name));	// in tag1 ex tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, setTag1, name));	// in tag2 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, setTag2, name));	// in tag2 ex tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, setTag3, name));	// in tag2 ex tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag1, name));	// in tag3 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag2, name));	// in tag3 ex tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag3, name));	// in tag3 ex tag3

		/*
		 * with 3 movies and tags
		 *
		 * movie1(tag1, tag2)
		 * movie2(tag1, tag2, tag3)
		 * movie3()
		 */
		movieTagDao.addTag(new HashSet<Movie>() { { add(movies.get(0)); } }, tag1);
		movieTagDao.addTag(new HashSet<Movie>() { { add(movies.get(0)); } }, tag2);
		movieTagDao.addTag(new HashSet<Movie>() { { add(movies.get(1)); } }, tag1);
		movieTagDao.addTag(new HashSet<Movie>() { { add(movies.get(1)); } }, tag2);
		movieTagDao.addTag(new HashSet<Movie>() { { add(movies.get(1)); } }, tag3);

		Assert.assertEquals(2, movieTagDao.countMovies(setTag1, EMPTY, name));		// in tag1
		Assert.assertEquals(2, movieTagDao.countMovies(setTag2, EMPTY, name));		// in tag2
		Assert.assertEquals(1, movieTagDao.countMovies(setTag3, EMPTY, name));		// in tag3
		Assert.assertEquals(2, movieTagDao.countMovies(setTag12, EMPTY, name));		// in tag1, tag2
		Assert.assertEquals(1, movieTagDao.countMovies(setTag13, EMPTY, name));		// in tag1, tag3
		Assert.assertEquals(1, movieTagDao.countMovies(setTag23, EMPTY, name));		// in tag2, tag3
		Assert.assertEquals(1, movieTagDao.countMovies(setTag123, EMPTY, name));	// in tag1, tag2, tag3

		Assert.assertEquals(1, movieTagDao.countMovies(EMPTY, setTag1, name));		// ex tag1
		Assert.assertEquals(1, movieTagDao.countMovies(EMPTY, setTag2, name));		// ex tag2
		Assert.assertEquals(2, movieTagDao.countMovies(EMPTY, setTag3, name));		// ex tag3
		Assert.assertEquals(1, movieTagDao.countMovies(EMPTY, setTag12, name));		// ex tag1, tag2
		Assert.assertEquals(1, movieTagDao.countMovies(EMPTY, setTag13, name));		// ex tag1, tag3
		Assert.assertEquals(1, movieTagDao.countMovies(EMPTY, setTag23, name));		// ex tag2, tag3
		Assert.assertEquals(1, movieTagDao.countMovies(EMPTY, setTag123, name));	// ex tag1, tag2, tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, setTag1, name));	// in tag1 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag1, setTag2, name));	// in tag1 ex tag2
		Assert.assertEquals(1, movieTagDao.countMovies(setTag1, setTag3, name));	// in tag1 ex tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, setTag1, name));	// in tag2 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag2, setTag2, name));	// in tag2 ex tag2
		Assert.assertEquals(1, movieTagDao.countMovies(setTag2, setTag3, name));	// in tag2 ex tag3

		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag1, name));	// in tag3 ex tag1
		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag2, name));	// in tag3 ex tag2
		Assert.assertEquals(0, movieTagDao.countMovies(setTag3, setTag3, name));	// in tag3 ex tag3
	}
}
