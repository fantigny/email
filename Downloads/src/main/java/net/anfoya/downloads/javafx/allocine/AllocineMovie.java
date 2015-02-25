package net.anfoya.downloads.javafx.allocine;

import com.google.gson.JsonObject;

public class AllocineMovie {
	private static final AllocineMovie EMPTY_MOVIE = new AllocineMovie();

	public static AllocineMovie getEmptyMovie() {
		return EMPTY_MOVIE;
	}

	private final String originalTitle;
	private final String frenchTitle;
	private final String thumbnail;
	private final String id;

	public AllocineMovie(final JsonObject jsonMovie) {
		if (jsonMovie.has("title2")) {
			originalTitle = jsonMovie.get("title2").getAsString();
		} else {
			originalTitle = "";
		}
		if (jsonMovie.has("title1")) {
			frenchTitle = jsonMovie.get("title1").getAsString();
		} else {
			frenchTitle = originalTitle;
		}
		if (jsonMovie.has("thumbnail")) {
			thumbnail = jsonMovie.get("thumbnail").getAsString();
		} else {
			thumbnail = "";
		}
		if (jsonMovie.has("id")) {
			id = jsonMovie.get("id").getAsString();
		} else {
			id = "";
		}
	}

	public AllocineMovie(final String title) {
		this.originalTitle = title;
		this.frenchTitle = "";
		this.thumbnail = "";
		this.id = "";
	}

	private AllocineMovie() {
		this("");
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof AllocineMovie) {
			final AllocineMovie other = (AllocineMovie) o;
			if (other.originalTitle.isEmpty()) {
				return originalTitle.isEmpty();
			} else {
				return other.originalTitle.equals(originalTitle) || other.originalTitle.equals(frenchTitle);
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return getOriginalTitle();
	}

	public String getFrenchTitle() {
		return frenchTitle;
	}

	public String getOriginalTitle() {
		return originalTitle;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public String getId() {
		return id;
	}
}