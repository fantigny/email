package net.anfoya.movie.browser.model;

import net.anfoya.tag.model.SimpleSection;

@SuppressWarnings("serial")
public class Section extends SimpleSection {
	public static final Section TO_WATCH = new Section(Tag.TO_WATCH_NAME);
	public static final Section FRENCH = new Section(Tag.FRENCH_NAME);
	public static final Section MEI_LIN = new Section(Tag.MEI_LIN_NAME);

	public Section(final String name) {
		super(name);
	}
}
