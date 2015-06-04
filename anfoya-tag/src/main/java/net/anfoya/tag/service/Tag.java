package net.anfoya.tag.service;

import java.io.Serializable;

import net.anfoya.tag.model.SimpleTag;

public interface Tag extends Serializable {
	public static final String NO_TAG_NAME = "[no tag]";
	public static final String TO_WATCH_NAME = "To watch";
	public static final String THIS_NAME = "[ this ]";

	public SimpleTag copyWithId(final String id);
	public String getId();
	public String getName();
	public boolean isSystem();
}
