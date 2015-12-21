package net.anfoya.movie.browser.javafx.taglist;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jgroups.JChannel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.saxsys.javafx.test.JfxRunner;
import de.saxsys.javafx.test.TestInJfxThread;
import javafx.scene.control.Labeled;
import net.anfoya.cluster.StatusManager;
import net.anfoya.cluster.UpdateManager;
import net.anfoya.movie.browser.dao.DataSource;
import net.anfoya.movie.browser.dao.MovieDao;
import net.anfoya.movie.browser.dao.MovieTagDao;
import net.anfoya.movie.browser.dao.TagDao;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Section;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.movie.browser.service.MovieTagService;
import net.anfoya.tag.javafx.scene.section.SectionPane;

@RunWith(JfxRunner.class)
public class SectionPaneTest {
	private static final Set<Tag> EMPTY_TAG_SET = new LinkedHashSet<Tag>();

	private static MovieTagService tagService;
	private static TagDao tagDao;
	private static MovieDao movieDao;
	private static MovieTagDao movieTagDao;

	@Ignore
	@BeforeClass
	public static void init() throws Exception {
		final DataSource dataSource = new DataSource();
		tagDao = new TagDao(dataSource);
		tagDao.init();
		movieDao = new MovieDao(dataSource);
		movieDao.init();
		movieTagDao  = new MovieTagDao(dataSource);
		movieTagDao.init();

		final UpdateManager updateMgr = new UpdateManager(new StatusManager("Section pane test", new JChannel()));

		tagService = new MovieTagService(updateMgr, tagDao, movieTagDao);
	}

	@Ignore
	@Test @TestInJfxThread @SuppressWarnings("serial")
	public void regularUpdateMovieCount() throws SQLException {

		final String name = "regular";
		final Section section = new Section(name + "Section");

		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(int i=0; i<3; i++) {
			final String tagName = name + i;

			final Set<Movie> movies = new LinkedHashSet<Movie>();
			for(int j=i; j<5; j++) {
				final String path = tagName + "Movie" + j;
				movieDao.add(new LinkedHashSet<Movie>() { { add(new Movie(path, -1)); } });
				movies.add(movieDao.find(path).iterator().next());
			}

			tagDao.add(new Tag(tagName, section.getName()));
			final Tag tag = tagDao.findByName(tagName);
			movieTagDao.addTag(movies, tag);
			tags.add(tag);
		}

		final SectionPane<Section, Tag> sectionPane = new SectionPane<Section, Tag>(section, tagService, true);
		sectionPane.refresh("", EMPTY_TAG_SET, EMPTY_TAG_SET, "");

		final Labeled title = (Labeled) sectionPane.getGraphic();
		Assert.assertEquals(section.getName(), title.getText());

		sectionPane.updateCountAsync(-1, tags, EMPTY_TAG_SET, EMPTY_TAG_SET, "", "");
		//TODO: async update prevent from validating result
//		Assert.assertEquals(section.getName() + " (12)", title.getText());
	}

}
