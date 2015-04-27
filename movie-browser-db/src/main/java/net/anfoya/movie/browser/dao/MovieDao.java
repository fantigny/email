package net.anfoya.movie.browser.dao;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MovieDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(MovieDao.class);

	private final DataSource dataSource;

	public MovieDao(final DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void init() throws SQLException {
		final String sql = "CREATE TABLE IF NOT EXISTS movie"
				+ "(	id			INT				NOT NULL AUTO_INCREMENT"
				+ ",	path		VARCHAR(512)	NOT NULL"
				+ ",	last_mod	BIGINT			NOT NULL"
				+ ",	urls		VARCHAR(1024)"
				+ ",	PRIMARY KEY (id)"
				+ ",	UNIQUE INDEX (path)"
				+ ");";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		statement.execute();
		statement.close();
		connection.close();
	}

	public void add(final Set<Movie> movies) throws SQLException {
		String sql = "INSERT"
				+ " INTO movie(path, last_mod, urls)"
				+ " VALUES ";
		for(int i=0, len=movies.size(); i<len; i++) {
			if (i != 0) {
				sql += ",";
			}
			sql += "(?,?,?)";
		}

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		int i=0;
		for(final Movie movie: movies) {
			statement.setString(++i, movie.getPath());
			statement.setLong(++i, movie.getLastModified());
			statement.setString(++i, new Gson().toJson(movie.getUrlMap()));
		}

		statement.executeUpdate();
		statement.close();
		connection.close();
	}

	public void del(final Set<Movie> movies) throws SQLException {
		String sql = "DELETE"
				+ " FROM movie"
				+ " WHERE";
		for(int i=0, len=movies.size(); i<len; i++) {
			if (i != 0) {
				sql += " OR";
			}
			sql += " id = ?";
		}

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		int i=0;
		for(final Movie movie: movies) {
			statement.setInt(++i, movie.getId());
		}

		statement.execute();
		statement.close();
		connection.close();
	}

	public Set<Movie> find(final String namePattern) throws SQLException {
		final Set<Tag> emptySet = new LinkedHashSet<Tag>();
		return find(emptySet, emptySet, emptySet, namePattern);
	}

	public Set<Movie> find(final Set<Tag> tags, final Set<Tag> includes, final Set<Tag> excludes, final String namePattern) throws SQLException {
		final boolean tagClause = !tags.isEmpty(), incClause = !includes.isEmpty(), excClause = !excludes.isEmpty(), nameClause = !namePattern.isEmpty();
		String sql = "SELECT m.id AS movie_id, m.path, m.last_mod, m.urls, mt.tag_id AS tag_id, t.name, t.section"
				+ " FROM movie m";
		if (!incClause) {
			sql += " LEFT";
		}
		sql += 	" JOIN movie_tag mt ON mt.movie_id = m.id";
		if (!incClause) {
			sql += " LEFT";
		}
		sql += 	" JOIN tag t ON t.id = mt.tag_id";
		if (tagClause || incClause || excClause || nameClause) {
			sql += " WHERE ";
		}
		if (tagClause) {
			sql += 	" t.id IN (";
			for(int i=0, n=tags.size(); i<n; i++) {
				if (i != 0) {
					sql += ",";
				}
				sql += "?";
			}
			sql += 	")";
		}
		if (incClause) {
			if (tagClause) {
				sql += " AND ";
			}
			sql += " ? = (SELECT COUNT(*)"
					+ " FROM movie_tag"
					+ " WHERE movie_id = mt.movie_id"
					+ " AND tag_id IN (";
			for(int i=0, n=includes.size(); i<n; i++) {
				if (i != 0) {
					sql += ",";
				}
				sql += "?";
			}
			sql += "))";
		}
		if (excClause) {
			if (tagClause || incClause) {
				sql += " AND ";
			}
			sql += " 0 = (SELECT COUNT(*)"
					+ " FROM movie_tag"
					+ " WHERE movie_id = mt.movie_id"
					+ " AND tag_id IN (";
			for(int i=0, n=excludes.size(); i<n; i++) {
				if (i != 0) {
					sql += ",";
				}
				sql += "?";
			}
			sql += "))";
		}
		if (nameClause) {
			if (tagClause || incClause || excClause) {
				sql += " AND ";
			}
			sql += " m.path LIKE CONCAT('%', ?, '%')";
		}
		sql += " ORDER BY movie_id, tag_id";

		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		int i=0;
		if (tagClause) {
			for(final Tag tag: tags) {
				statement.setInt(++i, tag.getId());
			}
		}
		if (incClause) {
			statement.setInt(++i, includes.size());
			for(final Tag tag: includes) {
				statement.setInt(++i, tag.getId());
			}
		}
		if (excClause) {
			for(final Tag tag: excludes) {
				statement.setInt(++i, tag.getId());
			}
		}
		if (nameClause) {
			statement.setString(++i, namePattern);
		}

		final ResultSet rs = statement.executeQuery();

		final Map<Integer, Movie> movieMap = new HashMap<Integer, Movie>();
		final Map<Integer, Set<Tag>> tagMap = new HashMap<Integer, Set<Tag>>();
		while(rs.next()) {
			final int movieId = rs.getInt("movie_id");
			if (!movieMap.containsKey(movieId)) {
				movieMap.put(movieId, new Movie(
						movieId
						, rs.getString("path")
						, rs.getLong("last_mod")
						, new Gson().fromJson(rs.getString("urls"), new TypeToken<Map<String, String>>() {}.getType())));
			}
			final int tagId = rs.getInt("tag_id");
			if (!rs.wasNull()) {
				if (!tagMap.containsKey(movieId)) {
					tagMap.put(movieId, new LinkedHashSet<Tag>());
				}
				tagMap.get(movieId).add(new Tag(
						tagId
						, rs.getString("name")
						, rs.getString("section")));
			}
		}
		rs.close();
		statement.close();
		connection.close();

		final Set<Movie> movies = new LinkedHashSet<Movie>();
		for(final Entry<Integer, Movie> entry: movieMap.entrySet()) {
			final int id = entry.getKey();
			final Movie movie = entry.getValue();
			if (tagMap.containsKey(id)) {
				final Set<Tag> movieTags = tagMap.get(id);
				movies.add(movie.copyWithTags(movieTags));
			} else {
				movies.add(movie);
			}
		}

		return movies;
	}

	public void updatePath(final Movie movie) throws SQLException {
		final String sql = "UPDATE movie"
				+ " SET path=?"
				+ " WHERE id=?";
		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		statement.setString(1, movie.getPath());
		statement.setInt(2, movie.getId());
		statement.execute();
		statement.close();
		connection.close();
	}

	public void updateLastModified(final Set<Movie> lastModifiedUpdatedMovies) throws SQLException {
		for(final Movie movie: lastModifiedUpdatedMovies){
			updateLastModified(movie);
		}
	}

	public void updateLastModified(final Movie movie) throws SQLException {
		final String sql = "UPDATE movie"
				+ " SET last_mod=?"
				+ " WHERE id=?";
		LOGGER.debug(sql);
		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		statement.setLong(1, movie.getLastModified());
		statement.setInt(2, movie.getId());
		statement.execute();
		statement.close();
		connection.close();
	}

	public void updateUrls(final Movie movie) throws SQLException {
		final String sql = "UPDATE movie"
				+ " SET urls=?"
				+ " WHERE id=?";
		LOGGER.debug(sql);

		final Connection connection = dataSource.getConnection();
		final PreparedStatement statement = connection.prepareStatement(sql);
		statement.setString(1, new Gson().toJson(movie.getUrlMap()));
		statement.setInt(2, movie.getId());
		statement.execute();
		statement.close();
		connection.close();
	}
}