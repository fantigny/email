package net.anfoya.mail.yahoo.model;

import javax.mail.Folder;

import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.model.Tag;

@SuppressWarnings("serial")
public class YahooTag extends SimpleTag implements Tag {
	public static final YahooTag INBOX = new YahooTag("Inbox", "inbox", "Yahoo/Inbox", true);
	public static final YahooTag UNREAD = new YahooTag("Unread", "unread", "Yahoo/Unread", true);
	public static final YahooTag STARRED = new YahooTag("Flagged", "flagged", "Yahoo/Flagged", true);
	public static final YahooTag DRAFT = new YahooTag("Drafts", "draft", "Yahoo/Drafts", true);
	public static final YahooTag SENT = new YahooTag("Sent", "sent", "Yahoo/Sent", true);
	public static final YahooTag ALL = new YahooTag("Archive", "all", "Yahoo/Archive", true);
	public static final YahooTag SPAM = new YahooTag("Spam", "spam", "Yahoo/Spam", true);
	public static final YahooTag TRASH = new YahooTag("Deleted Items", "trash", "Yahoo/Deleted Items", true);

	public YahooTag(String id, String name, String path, boolean system) {
		super(id, name, path, system);
	}

	public YahooTag(Folder f) {
		super(f.getFullName(), f.getFullName(), "Yahoo/" + f.getFullName(), false);
	}
}
