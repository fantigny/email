package net.anfoya.tag.service;

import java.io.Serializable;

import javafx.scene.input.DataFormat;


public interface Section extends Serializable {
	public static final DataFormat SECTION_DATA_FORMAT = new DataFormat("SECTION_DATA_FORMAT");

	public static final String NO_SECTION_NAME = "[to sort]";
	public static final String NO_ID = "section-with-no-id-";

	public String getName();
	public String getId();
	public boolean isSystem();
}
