package net.anfoya.movie.browser.service;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.util.Callback;
import net.anfoya.cluster.UpdateManager;
import net.anfoya.movie.browser.dao.MovieTagDao;
import net.anfoya.movie.browser.dao.TagDao;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Section;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.tag.model.SpecialTag;
import net.anfoya.tag.service.TagException;
import net.anfoya.tag.service.TagService;

public class MovieTagService implements TagService<Section, Tag> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MovieTagService.class);

	private final TagDao tagDao;
	private final MovieTagDao movieTagDao;

	private final UpdateManager updateMgr;

	public MovieTagService(final UpdateManager liveConsoService, final TagDao tagDao, final MovieTagDao movieTagDao) {
		updateMgr = liveConsoService;
		this.tagDao = tagDao;
		this.movieTagDao = movieTagDao;
	}

	public void addTagForMovies(final Tag tag, final Set<Movie> movies) {
		try {
			movieTagDao.addTag(movies, tag);
			updateMgr.updatePerformed();
		} catch (final SQLException e) {
			LOGGER.error("adding tag: {} for movies: {}", tag.getName(), movies.toArray(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error adding tag: " + tag.getName() + " for movies: " + movies.toArray());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}

	public void delTagsForMovies(final Set<Movie> movies) {
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		for(final Movie movie: movies) {
			tags.addAll(movie.getTags());
		}
		for(final Tag tag: tags) {
			delTagForMovies(tag, movies, false);
		}
	}

	public void delTagForMovies(final Tag tag, final Set<Movie> movies, final boolean addNoTag) {
		try {
			movieTagDao.delTag(movies, tag);
			updateMgr.updatePerformed();
		} catch (final SQLException e) {
			LOGGER.error("removing tag: {} for movies: {}", tag.getName(), movies.toString(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error removing tag: " + tag.getName() + " for movies: " + movies.toString());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}

		if (addNoTag) {
			final Set<Movie> noTagMovies = new LinkedHashSet<Movie>();
			for(final Movie movie: movies) {
				if (movie.getTags().size() == 1 && movie.getTags().iterator().next().equals(tag)) {
					noTagMovies.add(movie);
				}
			}

			if (!noTagMovies.isEmpty()) {
				final Tag noTag = findOrCreate(Tag.NO_TAG_NAME);
				addTagForMovies(noTag, noTagMovies);
			}
		}

		delOrphanTags();
	}

	@Override
	public Set<Tag> getTags(String pattern) {
		try {
			if (pattern.isEmpty()) {
				return tagDao.find();
			} else {
				return tagDao.find(pattern);
			}
		} catch (final SQLException e) {
			LOGGER.error("listing tags", e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error listing tags");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}
	}

	@Override
	public Set<Tag> getTags(Section section) {
		try {
			return tagDao.find(section);
		} catch (final SQLException e) {
			LOGGER.error("listing tags", e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error listing tags");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}
	}

	public int delOrphanTags() {
		int count = 0;
		try {
			count  = tagDao.delOrphanTags();
			if (count > 0) {
				updateMgr.updatePerformed();
			}
		} catch (final SQLException e) {
			LOGGER.error("deleting orphan tags", e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error deleting orphan tags");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
		}

		return count;
	}

	public Tag findOrCreate(final String name) {
		try{
			if (!tagDao.exists(name)) {
				if (Tag.NO_TAG_NAME.equals(name)) {
					tagDao.add(Tag.NO_TAG);
				} else if (Tag.TO_WATCH_NAME.equals(name)) {
					tagDao.add(Tag.TO_WATCH);
				} else {
					tagDao.add(new Tag(name, Section.NO_SECTION.getName()));
				}
				updateMgr.updatePerformed();
			}
			return tagDao.findByName(name);
		} catch (final SQLException e) {
			LOGGER.error("loading tag: {}", name, e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error loading tag: " + name);
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}
	}

	@Override
	public Set<Section> getSections() {
		try {
			return tagDao.findSections();
		} catch (final SQLException e) {
			LOGGER.error("listing sections", e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error listing sections");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}
	}

	@Override
	public Tag moveToSection(final Tag tag, final Section section) throws TagException {
		try {
			tagDao.updateSection(tag);
			updateMgr.updatePerformed();
			return tag;
		} catch (final SQLException e) {
			LOGGER.error("adding tag to section", e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error adding tag to section");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}
	}

	@Override
	public int getCountForTags(final Set<Tag> includes, final Set<Tag> excludes, final String pattern) throws TagException {
		try {
			return movieTagDao.countMovies(includes, excludes, pattern);
		} catch (final SQLException e) {
			LOGGER.error("counting movies with name pattern: %{}% and tags: ({}), exc: ({})", pattern, includes, excludes, e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error counting movies with name pattern: %" + pattern + "% and tags: " + includes.toString() + ", exc: " + excludes.toString());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return 0;
		}
	}

	@Override
	public int getCountForSection(Section section, Set<Tag> includes, Set<Tag> excludes, String itemPattern)
			throws TagException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Section addSection(String name) throws TagException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(Section Section) throws TagException {
		// TODO Auto-generated method stub

	}

	@Override
	public Section rename(Section Section, String name) throws TagException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void hide(Section Section) throws TagException {
		// TODO Auto-generated method stub

	}

	@Override
	public void show(Section Section) throws TagException {
		// TODO Auto-generated method stub

	}

	@Override
	public Tag findTag(String name) throws TagException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tag getTag(String id) throws TagException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Tag> getHiddenTags() throws TagException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tag getSpecialTag(SpecialTag specialTag) throws TagException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tag addTag(String name) throws TagException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(Tag tag) throws TagException {
		// TODO Auto-generated method stub

	}

	@Override
	public Tag rename(Tag tag, String name) throws TagException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void hide(Tag tag) throws TagException {
		// TODO Auto-generated method stub

	}

	@Override
	public void show(Tag tag) throws TagException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOnUpdateTagOrSection(Callback<Void, Void> callback) {
		// TODO Auto-generated method stub

	}
}
