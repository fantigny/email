package net.anfoya.movie.browser.model;


public class Tag {
	public static final String TO_WATCH_NAME = "To watch";
	public static final String FRENCH_NAME = "French";
	public static final String NO_TAG_NAME = "No tag :-(";
	public static final String MEI_LIN_NAME = "Mei Lin";

	public static final Tag TO_WATCH = new Tag(TO_WATCH_NAME, Section.TO_WATCH.getName());
	public static final Tag NO_TAG = new Tag(NO_TAG_NAME, Section.NO_SECTION.getName());

	private final int id;
	private final String name;
	private final String section;

	public Tag(final int id, final String name, final String section) {
		super();
		this.id = id;
		this.name = name;
		this.section = section;
	}
	public Tag(final String name, final String section) {
		this(-1, name, section);
	}
	@Override
	public int hashCode() {
	    return id;
	}
    @Override
	public boolean equals(final Object other) {
        if (other == null) return false;
        if (!this.getClass().equals(other.getClass())) return false;
        return ((Tag) other).id == id;
    }
    @Override
	public String toString() {
    	return name;
    }
	public Tag copyWithId(final int id) {
		return new Tag(id, name, section);
	}
	public Tag copyWithSection(final String section) {
		return new Tag(id, name, section);
	}
	public int getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getSection() {
		return section;
	}
}
