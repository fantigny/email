package net.anfoya.tag.model;



public class Tag implements Comparable<Tag> {
	public static final String TO_WATCH_NAME = "To watch";
	public static final String FRENCH_NAME = "French";
	public static final String NO_TAG_NAME = "No tag :-(";
	public static final String MEI_LIN_NAME = "Mei Lin";

	public static final Tag TO_WATCH = new Tag(TO_WATCH_NAME, Section.TO_WATCH.getName());
	public static final Tag NO_TAG = new Tag(NO_TAG_NAME, Section.NO_SECTION.getName());

	private final String id;
	private final String name;

	public Tag(final String id, final String name) {
		this.id = id;
		this.name = name;
	}
	@Override
	public int hashCode() {
	    return id.hashCode();
	}
    @Override
	public boolean equals(final Object other) {
        if (other == null) {
			return false;
		}
        if (!this.getClass().equals(other.getClass())) {
			return false;
		}
        return ((Tag) other).id.equals(id);
    }
    @Override
	public String toString() {
    	return name;
    }
	public Tag copyWithId(final String id) {
		return new Tag(id, name);
	}
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	@Override
	public int compareTo(final Tag o) {
		return name.compareTo(o.name);
	}
}
