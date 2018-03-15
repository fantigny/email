package net.anfoya.tag.model;

import net.anfoya.tag.service.Section;


@SuppressWarnings("serial")
public class SimpleSection implements Section, Comparable<SimpleSection> {
	public static final String NO_SECTION_NAME = "[to sort]";
	public static final String NO_ID = "section-with-no-id-";

	public static final SimpleSection NO_SECTION = new SimpleSection(NO_SECTION_NAME);
	public static final SimpleSection TO_WATCH = new SimpleSection(SimpleTag.TO_WATCH_NAME);

	private final String id;
	private final String name;
	private final boolean system;
	private final int hash;

	public SimpleSection(final String name) {
		this(NO_ID + name, name, true);
	}
	public SimpleSection(final String id, final String name, final boolean system) {
		this.id = id;
		this.name = name;
		this.system = system;
		hash = id.hashCode();
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public String toString() {
		return name;
	}
	@Override
	public int hashCode() {
	    return hash;
	}
    @Override
	public boolean equals(final Object other) {
        if (other == null) {
			return false;
		}
        if (!this.getClass().equals(other.getClass())) {
			return false;
		}
        return ((SimpleSection) other).name.equals(name);
    }
	@Override
	public String getId() {
		return id;
	}
	@Override
	public int compareTo(final SimpleSection o) {
		return name.compareTo(o.name);
	}
	@Override
	public boolean isSystem() {
		return system;
	}
}
