package net.anfoya.tag.model;



public class ThreadTag implements Comparable<ThreadTag> {
	public static final String NO_TAG_NAME = "No tag :-(";
	public static final String TO_WATCH_NAME = "To watch";

	private final String id;
	private final String name;
	private final int hashCode;

	public ThreadTag() {
		this("n/d", "n/d");
	}
	public ThreadTag(final String id, final String name) {
		this.id = id;
		this.name = name;
		this.hashCode = id.hashCode();
	}

    @Override
	public String toString() {
    	return name;
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
        return ((ThreadTag) other).id.equals(id);
    }

	@Override
	public int compareTo(final ThreadTag o) {
		return name.compareTo(o.name);
	}

	public ThreadTag copyWithId(final String id) {
		return new ThreadTag(id, name);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
