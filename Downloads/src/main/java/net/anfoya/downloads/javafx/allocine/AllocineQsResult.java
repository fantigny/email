package net.anfoya.downloads.javafx.allocine;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javafx.scene.image.Image;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AllocineQsResult {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineQsResult.class);
	private static final AllocineQsResult EMPTY_MOVIE = new AllocineQsResult();

	public static AllocineQsResult getEmptyResult() {
		return EMPTY_MOVIE;
	}

	private final String type;
	private final String id;
	private final String name;
	private final String director;
	private final String french;
	private final String year;
	private final String thumbnail;
	private final String activity;
	private final String creator;

	private final Future<Image> thumbnailFuture;
	private Image thumbnailImage;

	public AllocineQsResult(final JsonObject jsonResult) {
		this.id = getValue(jsonResult, "id");
		this.type = getValue(jsonResult, "entitytype");
		this.thumbnail = getValue(jsonResult, "thumbnail");
		this.director = getMetadata(jsonResult, "director");
		this.activity = getMetadata(jsonResult, "activity");
		this.creator = getMetadata(jsonResult, "creator");

		if (isPerson()) {
			name = getValue(jsonResult, "title1");
		} else {
			name = getValue(jsonResult, "title2");
		}

		if (isSerie()) {
			this.year = getMetadata(jsonResult, "yearstart");
		} else {
			this.year = getMetadata(jsonResult, "productionyear");
		}

		final String french = getValue(jsonResult, "title1");
		if (!french.isEmpty()) {
			this.french = french;
		} else {
			this.french = this.name;
		}

		if (!thumbnail.isEmpty()) {
			thumbnailFuture = ThreadPool.getInstance().submit(new Callable<Image>() {
				@Override
				public Image call() {
					return new Image(thumbnail);
				}
			});
		} else {
			thumbnailFuture = null;
		}
	}

	private String getValue(final JsonObject jsonObject, final String id) {
		try {
			return jsonObject.get(id).getAsString();
		} catch (final Exception e) {
			LOGGER.warn("not found {}", id);
		}
		return "";
	}

	private String getMetadata(final JsonObject jsonObject, final String id) {
		String metadata = "";
		try {
			final JsonArray jsonArray = jsonObject.get("metadata").getAsJsonArray();
			for (final JsonElement jsonElement : jsonArray) {
				final JsonObject jsonMetadata = jsonElement.getAsJsonObject();
				if (jsonMetadata.has("property") && jsonMetadata.get("property").getAsString().equals(id)) {
					if (!metadata.isEmpty()) {
						metadata += ", ";
					}
					metadata += jsonMetadata.get("value").getAsString();
				}
			}
		} catch (final Exception e) {
			LOGGER.warn("metadata not found {}", id);
		}
		return metadata;
	}

	public AllocineQsResult(final String name) {
		this.name = name;

		id = director = french = thumbnail = year = type = activity = creator ="";
		thumbnailFuture = null;
	}

	private AllocineQsResult() {
		this("");
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof AllocineQsResult) {
			final AllocineQsResult other = (AllocineQsResult) o;
			if (other.name.isEmpty()) {
				return name.isEmpty();
			} else {
				return other.name.equals(name) || other.name.equals(french);
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return getTitle();
	}

	public String getFrench() {
		return french;
	}

	public String getTitle() {
		return name;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public Image getThumbnailImage() {
		if (thumbnailImage == null && thumbnailFuture != null) {
			try {
				thumbnailImage = thumbnailFuture.get();
			} catch (final Exception e) {
				thumbnailImage = null;
			}
		}
		return thumbnailImage;
	}

	public String getId() {
		return id;
	}

	public String getDirector() {
		return director;
	}

	public String getYear() {
		return year;
	}

	public String getActivity() {
		return activity;
	}

	public String getType() {
		return type;
	}

	public boolean isPerson() {
		return type.equals("person");
	}

	public boolean isSerie() {
		return type.equals("series");
	}

	public String getCreator() {
		return creator;
	}
}