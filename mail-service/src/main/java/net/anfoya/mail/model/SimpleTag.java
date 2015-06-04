package net.anfoya.mail.model;

@SuppressWarnings("serial")
public class SimpleTag extends net.anfoya.tag.model.SimpleTag {
	public static final String STARRED = "STARRED";
	public static final String UNREAD = "UNREAD";

	public SimpleTag(final String id, final String name, final boolean system) {
		super(id, name, system);
	}
}
