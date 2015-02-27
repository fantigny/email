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

public class QuickSearchVo {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuickSearchVo.class);
	private static final QuickSearchVo EMPTY_QS_VO = new QuickSearchVo();

	public static QuickSearchVo getEmptyValue() {
		return EMPTY_QS_VO;
	}

	private final String type;
	private final String id;
	private final String name;
	private final String director;
	private final String french;
	private final String year;
	private final String activity;
	private final String creator;

	private final Future<Image> thumbnailFuture;
	private Image thumbnailImage;

	public QuickSearchVo(final JsonObject jsonResult) {
		this.id = getValue(jsonResult, "id");
		this.type = getValue(jsonResult, "entitytype");
		this.director = getMetadata(jsonResult, "director");
		this.activity = getMetadata(jsonResult, "activity");
		this.creator = getMetadata(jsonResult, "creator");

		if (isPerson()) {
			this.name = getValue(jsonResult, "title1");
		} else {
			this.name = getValue(jsonResult, "title2");
		}

		if (isSerie()) {
			this.year = getMetadata(jsonResult, "yearstart");
		} else {
			this.year = getMetadata(jsonResult, "productionyear");
		}

		this.french = getValue(jsonResult, "title1", this.name);

		final String thumbnail = getValue(jsonResult, "thumbnail", getClass().getResource("nothumbnail.png").toString());
		this.thumbnailFuture = ThreadPool.getInstance().submit(new Callable<Image>() {
			@Override
			public Image call() {
				return new Image(thumbnail);
			}
		});
	}

	private String getValue(final JsonObject jsonObject, final String id, final String... defaultVal) {
		String value;
		try {
			value = jsonObject.get(id).getAsString();
		} catch (final Exception e) {
			if (defaultVal.length != 0) {
				value = defaultVal[0];
			} else {
				value = "";
				LOGGER.warn("{} not found in {}", id, jsonObject.toString(), e);
			}
		}
		return value;
	}

	private String getMetadata(final JsonObject jsonObject, final String id, final String... defaultVal) {
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
			if (defaultVal.length != 0) {
				metadata = defaultVal[0];
			} else {
				metadata = "";
				LOGGER.warn("{} not found in {}", id, jsonObject.toString(), e);
			}
		}
		return metadata;
	}

	public QuickSearchVo(final String name) {
		this.name = name;

		id = director = french = year = type = activity = creator ="";
		thumbnailFuture = null;
	}

	private QuickSearchVo() {
		this("");
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof QuickSearchVo) {
			final QuickSearchVo other = (QuickSearchVo) o;
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

	public boolean isEmpty() {
		return equals(EMPTY_QS_VO);
	}
}