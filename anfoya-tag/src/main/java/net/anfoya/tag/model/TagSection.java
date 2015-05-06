package net.anfoya.tag.model;


public class TagSection implements Comparable<TagSection> {
	protected static final String NO_SECTION_NAME = "No section :-(";

	public static final TagSection NO_SECTION = new TagSection(NO_SECTION_NAME);
	public static final TagSection TO_WATCH = new TagSection(ThreadTag.TO_WATCH_NAME);

	private final String id;
	private final String name;
	private final int hash;

	public TagSection(final String name) {
		this(name, name);
	}
	public TagSection(final String id, final String name) {
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
        return ((TagSection) other).name.equals(name);
    }
	public String getId() {
		return id;
	}
	@Override
	public int compareTo(final TagSection o) {
		return name.compareTo(o.name);
	}
}
