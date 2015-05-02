package net.anfoya.mail.model;

public class Tag {

	private final String id;
	private final String name;

	public Tag(final String id, final String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString() {
		return getName();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
