package net.anfoya.mail.model;

import net.anfoya.mail.service.Tag;

@SuppressWarnings("serial")
public class SimpleTag extends net.anfoya.tag.model.SimpleTag implements Tag {

	public SimpleTag(final String id, final String name, final boolean system) {
		super(id, name, system);
	}

}
