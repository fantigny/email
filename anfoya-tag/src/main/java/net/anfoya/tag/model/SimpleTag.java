package net.anfoya.tag.model;

import java.io.Serializable;


@SuppressWarnings("serial")
public class SimpleTag implements Serializable, Comparable<SimpleTag> {
	public static final String NO_TAG_NAME = "[no tag]";
	public static final String TO_WATCH_NAME = "To watch";
	public static final String THIS_NAME = "[this]";

	private final String id;
	private final String name;
	private final int hash;

	public SimpleTag() {
		this("n/d", "n/d");
	}
	public SimpleTag(final String id, final String name) {
		this.id = id;
		this.name = name;
		this.hash = id.hashCode();
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
        return ((SimpleTag) other).id.equals(id);
    }

	@Override
	public int compareTo(final SimpleTag o) {
		return name.compareTo(o.name);
	}

	public SimpleTag copyWithId(final String id) {
		return new SimpleTag(id, name);
	}

	//TODO should not need an id?
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
