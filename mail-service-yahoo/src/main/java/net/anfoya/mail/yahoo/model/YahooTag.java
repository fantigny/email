package net.anfoya.mail.yahoo.model;

import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.model.Tag;

@SuppressWarnings("serial")
public class YahooTag extends SimpleTag implements Tag {

	public YahooTag(String id, String name, String path, boolean system) {
		super(id, name, path, system);
	}

}
