package net.anfoya.tag.service;

import java.io.Serializable;


public interface Section extends Serializable {
	public static final String NO_SECTION_NAME = "[to sort]";
	public static final String NO_ID = "section-with-no-id-";

	public String getName();
	public String getId();
	public boolean isSystem();
}
