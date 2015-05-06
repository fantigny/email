package net.anfoya.movie.browser.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

import net.anfoya.movie.browser.model.Section;
import net.anfoya.movie.browser.model.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagDao.class);

	private final DataSource dataSource;

	public TagDao(final DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void init() throws SQLException {
		final String sql = "CREATE TABLE IF NOT EXISTS tag"
				+ "(	id			INT				NOT NULL AUTO_INCREMENT"
				+ ",	name		VARCHAR(64)		NOT NULL"
				+ ",	section		VARCHAR(64)"
				+ ",	PRIMARY KEY (id)"
				+ ",	UNIQUE INDEX (name)"
				+ ");";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		statement.execute();
		statement.close();
		connection.close();
	}

	public boolean exists(final String name) throws SQLException {
		final String sql = "SELECT 1"
				+ " FROM tag"
				+ " WHERE name = ?";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		int i = 0;
		statement.setString(++i, name);
		final ResultSet rs = statement.executeQuery();
		final boolean exist = rs.next();
		rs.close();
		statement.close();
		connection.close();

		return exist;
	}

	public void add(final Tag tag) throws SQLException {
		final String sql = "INSERT"
				+ " INTO tag(name,section)"
				+ " VALUES (?,?)";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		int i = 0;
		statement.setString(++i, tag.getName());
		statement.setString(++i, tag.getSection());
		statement.executeUpdate();
		statement.close();
		connection.close();
	}

	public int delOrphanTags() throws SQLException {
		final String sql = "DELETE"
				+ " FROM tag"
				+ " WHERE name IN (SELECT temp.name FROM ("
					+ " SELECT t.name"
					+ " FROM tag t"
					+ " LEFT JOIN movie_tag mt ON mt.tag_id = t.id"
					+ " WHERE mt.movie_id IS NULL) temp)";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		statement.execute();
		final int count = statement.getUpdateCount();
		statement.close();
		connection.close();

		return count;
	}

	public Tag find(final String tagName) throws SQLException {
		final String sql = "SELECT id, name, section"
				+ " FROM tag"
				+ " WHERE name = ?";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		statement.setString(1, tagName);
		final ResultSet rs = statement.executeQuery();
		Tag tag = null;
		if (rs.next()) {
			tag = new Tag(rs.getInt("id"), tagName, rs.getString("section"));
		}

		rs.close();
		statement.close();
		connection.close();
		return tag;
	}

	public Set<Tag> find() throws SQLException {
		final String sql = "SELECT id, name, section"
				+ " FROM tag"
				+ " ORDER BY CASE"
					+ " WHEN name = ? THEN 1"
					+ " WHEN name = ? THEN 2"
					+ " ELSE 3"
					+ " END"
				+ " , name";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		int i = 0;
		statement.setString(++i, Tag.NO_TAG_NAME);
		statement.setString(++i, Tag.TO_WATCH_NAME);
		final ResultSet rs = statement.executeQuery();
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		while (rs.next()) {
			tags.add(new Tag(
					rs.getInt("id")
					, rs.getString("name")
					, rs.getString("section")));
		}

		rs.close();
		statement.close();
		connection.close();
		return tags;
	}

	public Set<Tag> find(final Section section, final String tagPattern) throws SQLException {
		final String sql = "SELECT id, name"
				+ " FROM tag"
				+ " WHERE section = ?"
				+ " AND name LIKE CONCAT('%', ?, '%')"
				+ " ORDER BY CASE"
					+ " WHEN name = ? THEN 1"
					+ " WHEN name = ? THEN 2"
					+ " ELSE 3"
					+ " END"
				+ " , name";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		int i = 0;
		statement.setString(++i, section.getName());
		statement.setString(++i, tagPattern);
		statement.setString(++i, Tag.NO_TAG_NAME);
		statement.setString(++i, Tag.TO_WATCH_NAME);
		final ResultSet rs = statement.executeQuery();
		final Set<Tag> tags = new LinkedHashSet<Tag>();
		while (rs.next()) {
			tags.add(new Tag(rs.getInt("id"), rs.getString("name"), section.getName()));
		}

		rs.close();
		statement.close();
		connection.close();
		return tags;
	}

	public Set<Section> findSections() throws SQLException {
		final String sql = "SELECT DISTINCT(section)"
				+ " FROM tag"
				+ " ORDER BY CASE"
					+ " WHEN section = ? THEN 1"
					+ " WHEN section = ? THEN 2"
					+ " WHEN section = ? THEN 3"
					+ " ELSE 4"
					+ " END"
				+ " , section";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		int i = 0;
		statement.setString(++i, Section.NO_SECTION.getName());
		statement.setString(++i, Section.TO_WATCH.getName());
		statement.setString(++i, Section.FRENCH.getName());
		final ResultSet rs = statement.executeQuery();
		final Set<Section> sections = new LinkedHashSet<Section>();
		while (rs.next()) {
			sections.add(new Section(rs.getString("section")));
		}

		rs.close();
		statement.close();
		connection.close();
		return sections;
	}

	public void updateSection(final Tag tag) throws SQLException {
		final String sql = "UPDATE tag"
				+ " SET section = ?"
				+ " WHERE id = ?";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		statement.setString(1, tag.getSection());
		statement.setInt(2, tag.getIntId());
		statement.executeUpdate();
		statement.close();
		connection.close();
	}
}
