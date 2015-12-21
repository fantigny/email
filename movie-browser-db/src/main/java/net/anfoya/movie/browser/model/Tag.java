package net.anfoya.movie.browser.model;

import net.anfoya.tag.model.SimpleTag;

@SuppressWarnings("serial")
public class Tag extends SimpleTag {
	public static final String TO_WATCH_NAME = "To watch";
	public static final String FRENCH_NAME = "French";
	public static final String NO_TAG_NAME = "No tag :-(";
	public static final String MEI_LIN_NAME = "Mei Lin";

	public static final Tag TO_WATCH = new Tag(TO_WATCH_NAME, Section.TO_WATCH.getName());
	public static final Tag NO_TAG = new Tag(NO_TAG_NAME, Section.NO_SECTION.getName());

	private final int id;
	private final String section;

	public Tag(final int id, final String name, final String section) {
		super("" + id, name, name, false);
		this.id = id;
		this.section = section;
	}
	public Tag(final String name, final String section) {
		this(-1, name, section);
	}
	public String getSection() {
		return section;
	}
	public int getIntId() {
		return id;
	}
	public Tag copyWithSection(final String section) {
		return new Tag(id, getName(), section);
	}
}
