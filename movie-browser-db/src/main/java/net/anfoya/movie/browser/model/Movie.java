package net.anfoya.movie.browser.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Movie {

	private final int id;
	private final String path;
	private final String name;
	private final long lastModified;
	private final Set<Tag> tags;
	private final Map<String, String> urlMap; // (websiteName, url)

	public enum SortOrder {
		DATE(new Comparator<Movie>() {
			@Override
			public int compare(final Movie m1, final Movie m2) {
				return Long.valueOf(m2.getLastModified()).compareTo(m1.getLastModified());
			}
		}),
		NAME(new Comparator<Movie>() {
			@Override
			public int compare(final Movie m1, final Movie m2) {
				return m1.getName().compareTo(m2.getName());
			}
		});

		private Comparator<Movie> comparator;
		private SortOrder(final Comparator<Movie> comparator) {
			this.comparator = comparator;
		}

		public Comparator<Movie> getComparator() {
			return comparator;
		}
	}

	public Movie(final int id, final String path, final long lastModified, final Set<Tag> movieTags, final Map<String, String> urlMap) {
		this.id = id;
		this.path = path;
		this.name = path.substring(path.lastIndexOf('/')+1).replaceFirst("[.][^.]+$", "");
		this.lastModified = lastModified;
		this.tags = Collections.unmodifiableSet(movieTags);
		this.urlMap = Collections.unmodifiableMap(urlMap);
	}
	public Movie(final int id, final String path, final long lastModified, final Map<String, String> urlMap) {
		this(id, path, lastModified, new LinkedHashSet<Tag>(), urlMap);
	}
	public Movie(final String path, final long lastModified) {
		this(-1, path, lastModified, new HashMap<String, String>());
	}

	@Override
	public String toString() {
		return getName();
	}
    @Override
	public boolean equals(final Object other) {
        if (other == null) return false;
        if (!this.getClass().equals(other.getClass())) return false;
        return ((Movie) other).id == id;
    }

	public Movie copyWithId(final int id) {
		return new Movie(id, path, lastModified, tags, urlMap);
	}
	public Movie copyWithPath(final String path) {
		return new Movie(id, path, lastModified, tags, urlMap);
	}
	public Movie copyWithLastModified(final long lastModified) {
		return new Movie(id, path, lastModified, tags, urlMap);
	}
	public Movie copyWithUrlMap(final Map<String, String> urlMap) {
		return new Movie(id, path, lastModified, tags, urlMap);
	}
	public Movie copyWithTags(final Set<Tag> movieTags) {
		return new Movie(id, path, lastModified, movieTags, urlMap);
	}

	public String getPath() {
		return path;
	}
	public String getName() {
		return name;
	}
	public int getId() {
		return id;
	}
	public Set<Tag> getTags() {
		return tags;
	}
	public long getLastModified() {
		return lastModified;
	}
	public Map<String, String> getUrlMap() {
		return urlMap;
	}
}
