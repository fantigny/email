package net.anfoya.movies.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

import junit.framework.Assert;
import net.anfoya.movies.dao.DataSource;
import net.anfoya.movies.dao.MovieDao;
import net.anfoya.movies.dao.MovieTagDao;
import net.anfoya.movies.dao.TagDao;
import net.anfoya.movies.model.Section;
import net.anfoya.movies.model.Tag;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TagDaoTest {

	private static DataSource dataSource;
	private static TagDao tagDao;

	@BeforeClass
	public static void init() throws SQLException, IOException {
		dataSource = new DataSource();
		tagDao = new TagDao(dataSource);
		tagDao.init();

		new MovieDao(dataSource).init();
		new MovieTagDao(dataSource).init();
	}

	@AfterClass
	public static void shutdown() throws SQLException {
		dataSource.close();
	}

	@Test
	public void add() throws SQLException {
		final String tagName = "add";
		final String sectionName = tagName + "Section";

		Tag tag = tagDao.find(tagName);
		Assert.assertNull(tag);

		tagDao.add(new Tag(tagName, sectionName));

		tag = tagDao.find(tagName);
		Assert.assertNotNull(tag);
		Assert.assertEquals(tagName, tag.getName());
		Assert.assertEquals(sectionName, tag.getSection());
	}

	@Test
	public void exists() throws SQLException {
		final String tagName = "exist";
		final String sectionName = tagName + "Section";

		boolean exists = tagDao.exists(tagName);
		Assert.assertFalse(exists);

		tagDao.add(new Tag(tagName, sectionName));

		exists = tagDao.exists(tagName);
		Assert.assertTrue(exists);
	}

	@Test
	public void delOrphanTags() throws SQLException {
		final String tagName = "delOrphanTags";
		final String sectionName = tagName + "Section";

		tagDao.delOrphanTags();

		int count = tagDao.delOrphanTags();
		Assert.assertEquals(0, count);

		Tag tag = tagDao.find(tagName);
		Assert.assertNull(tag);

		tagDao.add(new Tag(tagName, sectionName));

		tag = tagDao.find(tagName);
		Assert.assertNotNull(tag);

		count = tagDao.delOrphanTags();
		Assert.assertEquals(1, count);

		tag = tagDao.find(tagName);
		Assert.assertNull(tag);
	}

	@Test
	public void find() throws SQLException {
		final String tagName = "find";
		final String sectionName = tagName + "Section";
		final Section section = new Section(sectionName);

		Tag tag = tagDao.find(tagName);
		Assert.assertNull(tag);

		Set<Tag> tags = tagDao.find(section);
		Assert.assertEquals(0, tags.size());

		tagDao.add(new Tag(tagName, sectionName));

		tag = tagDao.find(tagName);
		Assert.assertNotNull(tag);

		tags = tagDao.find(section);
		Assert.assertEquals(1, tags.size());

		tags = tagDao.find();
		Assert.assertTrue(tags.contains(tag));
	}

	@Test
	public void updateSection() throws SQLException {
		final String tagName = "updateSection";
		final String sectionName = tagName + "Section";

		Tag tag = tagDao.find(tagName);
		Assert.assertNull(tag);

		tagDao.add(new Tag(tagName, sectionName));

		tag = tagDao.find(tagName);
		Assert.assertNotNull(tag);

		final String newSection = "newSection";
		tagDao.updateSection(tag.copyWithSection(newSection));

		tag = tagDao.find(tagName);
		Assert.assertEquals(newSection, tag.getSection());
	}

	@Test
	public void findSections() throws SQLException {
		final String name = "findSections";

		Set<Section> sections = tagDao.findSections();
		final int nbSection = sections.size();

		final int nbNew = 5;
		for(int i=0; i<nbNew; i++){
			tagDao.add(new Tag(name + i, name + i));
		}

		sections = tagDao.findSections();
		Assert.assertEquals(nbSection + nbNew, sections.size());

		int count = 0;
		for(int i=0; i<nbNew; i++){
			if (sections.contains(new Section(name + i))) {
				count++;
			}
		}
		Assert.assertEquals(nbNew, count);
	}
}