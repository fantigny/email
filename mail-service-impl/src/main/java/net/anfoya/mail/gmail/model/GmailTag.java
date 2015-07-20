package net.anfoya.mail.gmail.model;

import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.service.Tag;

import com.google.api.services.gmail.model.Label;

@SuppressWarnings("serial")
public class GmailTag extends SimpleTag implements Tag {
	public static final GmailTag DRAFT_TAG = new GmailTag("DRAFT", "Draft", "DRAFT", true);
	public static final GmailTag ALL_TAG = new GmailTag("ALL", "All mail", "ALL", true);
	public static final GmailTag UNREAD_TAG = new GmailTag("UNREAD", "Unread", "UNREAD", true);
	public static final GmailTag INBOX_TAG = new GmailTag("INBOX", "Inbox", "INBOX", true);
	public static final GmailTag SENT_TAG = new GmailTag("SENT", "Sent", "SENT", true);
	public static final GmailTag STARRED_TAG = new GmailTag("STARRED", "Flagged", "STARRED", true);

	private final String path;

	public static boolean isHidden(final Label label) {
		return "labelHide".equals(label.getLabelListVisibility())
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
			if (ALL_TAG.getId().equals(label.getId())) {
				name = ALL_TAG.getName();
			} else if (UNREAD_TAG.getId().equals(label.getId())) {
				name = UNREAD_TAG.getName();
			} else if (INBOX_TAG.getId().equals(label.getId())) {
				name = INBOX_TAG.getName();
			} else if (SENT_TAG.getId().equals(label.getId())) {
				name = SENT_TAG.getName();
			} else if (STARRED_TAG.getId().equals(label.getId())) {
				name = STARRED_TAG.getName();
			}
		}
		return name;
	}

	public GmailTag(final Label label) {
		this(label.getId(), getName(label), label.getName(), isSystem(label));
	}

	public GmailTag(final String id, final String name, final String path, final boolean system) {
		super(id, name, system);
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
