package net.anfoya.movies.service;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import net.anfoya.cluster.UpdateManager;
import net.anfoya.movies.dao.MovieTagDao;
import net.anfoya.movies.dao.TagDao;
import net.anfoya.movies.model.Movie;
import net.anfoya.movies.model.Section;
import net.anfoya.movies.model.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagService {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagService.class);

	private final TagDao tagDao;
	private final MovieTagDao movieTagDao;

	private final UpdateManager updateMgr;

	public TagService(final UpdateManager liveConsoService, final TagDao tagDao, final MovieTagDao movieTagDao) {
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

	public Set<Tag> getAllTags() {
		Set<Tag> tags;
		try {
			tags = tagDao.find();
		} catch (final SQLException e) {
			LOGGER.error("listing tags", e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error listing tags");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}

		return tags;
	}

	public Set<Tag> getTags(final Section section) {
		Set<Tag> tags;
		try {
			tags = tagDao.find(section);
		} catch (final SQLException e) {
			LOGGER.error("finding tags for section: {}", section.getName(), e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error finding tags for section: " + section.getName());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}

		return tags;
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
			return tagDao.find(name);
		} catch (final SQLException e) {
			LOGGER.error("loading tag: {}", name, e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error loading tag: " + name);
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return null;
		}
	}

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

	public void addToSection(final Tag tag) {
		try {
			tagDao.updateSection(tag);
			updateMgr.updatePerformed();
		} catch (final SQLException e) {
			LOGGER.error("adding tag to section", e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error adding tag to section");
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return;
		}
	}

	public int getSectionMovieCount(final Section section, final Set<Tag> tags, final Set<Tag> excludes, final String pattern) {
		try {
			return movieTagDao.countSectionMovies(section, tags, excludes, pattern);
		} catch (final SQLException e) {
			LOGGER.error("counting movies for section: {}, name pattern {} and tags: ({}), exc: ({})", section.getName(), pattern, tags, excludes, e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error counting movies for section: " + section.getName() + ", name pattern " + pattern + " and tags: " + tags.toString() + ", exc: " + excludes.toString());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return 0;
		}
	}

	public int getMovieCount(final Set<Tag> tags, final Set<Tag> excludes, final String pattern) {
		try {
			return movieTagDao.countMovies(tags, excludes, pattern);
		} catch (final SQLException e) {
			LOGGER.error("counting movies with name pattern: %{}% and tags: ({}), exc: ({})", pattern, tags, excludes, e);
			final Alert alertDialog = new Alert(AlertType.ERROR);
			alertDialog.setHeaderText("error counting movies with name pattern: %" + pattern + "% and tags: " + tags.toString() + ", exc: " + excludes.toString());
			alertDialog.setContentText(e.getMessage());
			alertDialog.show();
			return 0;
		}
	}
}
