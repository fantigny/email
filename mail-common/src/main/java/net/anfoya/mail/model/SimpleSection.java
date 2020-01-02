package net.anfoya.mail.model;

@SuppressWarnings("serial")
public class SimpleSection extends net.anfoya.tag.model.SimpleSection implements Section {

	public SimpleSection(final String name) {
		super(name);
	}

	public SimpleSection(final String id, final String name, final boolean system) {
		super(id, name, system);
	}
}
