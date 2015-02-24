package net.anfoya.downloads.javafx.allocine;

import com.google.gson.JsonObject;

public class AllocineMovie {
	private final String frenchTitle;
	private final String originalTitle;
	private final String thumbnail;

	public AllocineMovie(final JsonObject jsonMovie) {
		if (jsonMovie.has("title1")) {
			frenchTitle = jsonMovie.get("title1").getAsString();
		} else if (jsonMovie.has("title2")) {
			frenchTitle = jsonMovie.get("title2").getAsString();
		} else {
			frenchTitle = "";
		}
		if (jsonMovie.has("title1") && jsonMovie.has("title2")) {
			originalTitle = jsonMovie.get("title2").getAsString();
		} else {
			originalTitle = "";
		}
		if (jsonMovie.has("thumbnail")) {
			thumbnail = jsonMovie.get("thumbnail").getAsString();
		} else {
			thumbnail = "";
		}
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

	@Override
	public String toString() {
		return getFrenchTitle();
	}
}