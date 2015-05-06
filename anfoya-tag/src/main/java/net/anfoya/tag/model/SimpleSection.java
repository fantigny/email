package net.anfoya.tag.model;


public class SimpleSection implements Comparable<SimpleSection> {
	protected static final String NO_SECTION_NAME = "No section :-(";

	public static final SimpleSection NO_SECTION = new SimpleSection(NO_SECTION_NAME);
	public static final SimpleSection TO_WATCH = new SimpleSection(SimpleTag.TO_WATCH_NAME);

	private final String id;
	private final String name;
	private final int hash;

	public SimpleSection(final String name) {
		this(name, name);
	}
	public SimpleSection(final String id, final String name) {
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
        return ((SimpleSection) other).name.equals(name);
    }
	public String getId() {
		return id;
	}
	@Override
	public int compareTo(final SimpleSection o) {
		return name.compareTo(o.name);
	}
}
