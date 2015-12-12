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

	public static boolean isHidden(final Label label) {
		return label != null
				&& "labelHide".equals(label.getLabelListVisibility())
				|| "CATEGORY_PERSONAL".equals(label.getName());
	}

	public static boolean isSystem(final Label label) {
		return "system".equals(label.getType());
	}

	public static String getName(final Label label) {
		String name = label.getName();
		if (name.contains("/") && name.length() > 1) {
			name = name.substring(name.lastIndexOf("/") + 1);
		}
		if (isSystem(label)) {
			name = name.contains("CATEGORY_")? name.substring(9): name;
			name = name.charAt(0) + name.substring(1).toLowerCase();
			final String labelId = label.getId();
			if (ALL.getId().equals(labelId)) {
				name = ALL.getName();
			} else if (UNREAD.getId().equals(labelId)) {
				name = UNREAD.getName();
			} else if (INBOX.getId().equals(labelId)) {
				name = INBOX.getName();
			} else if (SENT.getId().equals(labelId)) {
				name = SENT.getName();
			} else if (STARRED.getId().equals(labelId)) {
				name = STARRED.getName();
			}
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
