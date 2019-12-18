package net.anfoya.mail.gmail.model;

import com.google.api.services.gmail.model.Label;

import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.service.Tag;

@SuppressWarnings("serial")
public class GmailTag extends SimpleTag implements Tag {
	public static final GmailTag DRAFT = new GmailTag("DRAFT", "draft", "DRAFT", true);
	public static final GmailTag ALL = new GmailTag("ALL", "all", "ALL", true);
	public static final GmailTag UNREAD = new GmailTag("UNREAD", "unread", "UNREAD", true);
	public static final GmailTag INBOX = new GmailTag("INBOX", "inbox", "INBOX", true);
	public static final GmailTag SENT = new GmailTag("SENT", "sent", "SENT", true);
	public static final GmailTag STARRED = new GmailTag("STARRED", "flagged", "STARRED", true);
	public static final GmailTag SPAM = new GmailTag("SPAM", "spam", "SPAM", true);
	public static final GmailTag TRASH = new GmailTag("TRASH", "trash", "TRASH", true);
	public static final GmailTag CHAT = new GmailTag("CHAT", "chat", "CHAT", true);

	public static final GmailTag PROMOTIONS = new GmailTag("CATEGORY_PROMOTIONS", "promotions", "PROMOTIONS", true);
	public static final GmailTag SOCIAL = new GmailTag("CATEGORY_SOCIAL", "social", "SOCIAL", true);
	public static final GmailTag UPDATES = new GmailTag("CATEGORY_UPDATES", "updates", "UPDATES", true);
	public static final GmailTag FORUMS = new GmailTag("CATEGORY_FORUMS", "forums", "FORUMS", true);

	public static final String SYSTEM = "system";
	public static final String HIDDEN = "labelHide";
	public static final String CAT_PERSONAL = "CATEGORY_PERSONAL";

	public static boolean isSystem(final Label label) {
		return SYSTEM.equals(label.getType());
	}

	public static boolean isHidden(final Label label) {
		return HIDDEN.equals(label.getLabelListVisibility())
				|| CAT_PERSONAL.equals(label.getName());
	}

	public static String getName(final Label label) {
		String name = label.getName();
		if (name.contains("/") && name.length() > 1) {
			name = name.substring(name.lastIndexOf("/") + 1);
		}
		return name;
	}

	public GmailTag(final Label label) {
		this(label.getId(), getName(label), label.getName(), isSystem(label));
	}

	public GmailTag(final String id, final String name, final String path, final boolean system) {
		super(id, name, path, system);
	}
}
