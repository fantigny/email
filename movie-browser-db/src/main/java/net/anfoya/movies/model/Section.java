package net.anfoya.movies.model;

public class Section {
	private static final String NO_SECTION_NAME = "No section :-(";

	public static final Section NO_SECTION = new Section(NO_SECTION_NAME);
	public static final Section TO_WATCH = new Section(Tag.TO_WATCH_NAME);
	public static final Section FRENCH = new Section(Tag.FRENCH_NAME);
	public static final Section MEI_LIN = new Section(Tag.MEI_LIN_NAME);

	private final String name;
	private int hash;
	public Section(String name) {
		this.name = name;
		this.hash = name.hashCode();
	}
	public String getName() {
		return name;
	}
	public String toString() {
		return name;
	}
	public int hashCode() {
	    return hash;
	}
    public boolean equals(Object other) {
        if (other == null) return false;
        if (!this.getClass().equals(other.getClass())) return false;
        return ((Section) other).name.equals(name);
    }
}
