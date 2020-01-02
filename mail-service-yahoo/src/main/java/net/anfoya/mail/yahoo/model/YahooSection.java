package net.anfoya.mail.yahoo.model;

import net.anfoya.mail.model.Section;
import net.anfoya.mail.model.SimpleSection;

@SuppressWarnings("serial")
public class YahooSection extends SimpleSection implements Section {
	public static final YahooSection SYSTEM = new YahooSection("Yahoo");
	public static final YahooSection FOLDERS = new YahooSection("Folders");

	public YahooSection(String name) {
		super(name);
	}
}
