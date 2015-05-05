package net.anfoya.tag.model;



public class Tag implements Comparable<Tag> {
	public static final String NO_TAG_NAME = "No tag :-(";
	public static final String TO_WATCH_NAME = "To watch";

	private final String id;
	private final String name;
	private final int hashCode;

	public Tag() {
		this("n/d", "n/d");
	}
	public Tag(final String id, final String name) {
		this.id = id;
		this.name = name;
		this.hashCode = id.hashCode();
	}
	@Override
	public int hashCode() {
	    return hashCode;
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
