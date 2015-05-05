package net.anfoya.tag.model;


public class Section implements Comparable<Section> {
	protected static final String NO_SECTION_NAME = "No section :-(";

	public static final Section NO_SECTION = new Section(NO_SECTION_NAME);
	public static final Section TO_WATCH = new Section(Tag.TO_WATCH_NAME);

	private final String id;
	private final String name;
	private final int hash;

	public Section(final String name) {
		this(name, name);
	}
	public Section(final String id, final String name) {
		this.id = id;
		this.name = name;
		hash = id.hashCode();
	}
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
        return ((Section) other).name.equals(name);
    }
	public String getId() {
		return id;
	}
	@Override
	public int compareTo(final Section o) {
		return name.compareTo(o.name);
	}
}
