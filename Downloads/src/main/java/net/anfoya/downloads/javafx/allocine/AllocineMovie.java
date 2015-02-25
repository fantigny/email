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
	}

	public AllocineMovie(final String title) {
		this.originalTitle = title;
		this.frenchTitle = "";
		this.thumbnail = "";
	}

	private AllocineMovie() {
		this("");
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof AllocineMovie) {
			final AllocineMovie other = (AllocineMovie) o;
			if (other.originalTitle.equals(originalTitle) || other.originalTitle.equals(frenchTitle)) {
				return true;
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
}