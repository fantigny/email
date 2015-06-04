package net.anfoya.mail.gmail.model;

import net.anfoya.mail.model.SimpleTag;

import com.google.api.services.gmail.model.Label;

@SuppressWarnings("serial")
public class GmailTag extends SimpleTag {
	public static final GmailTag ALL_MAIL = new GmailTag("ALL", "All mail", "ALL", true);
	public static final GmailTag UNREAD = new GmailTag("UNREAD", "Unread", "UNREAD", true);
	public static final GmailTag INBOX = new GmailTag("INBOX", "Inbox", "INBOX", true);
	public static final GmailTag SENT = new GmailTag("SENT", "Sent", "SENT", true);

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
