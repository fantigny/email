package net.anfoya.mail.gmail.model;

import net.anfoya.tag.model.SimpleTag;

import com.google.api.services.gmail.model.Label;

@SuppressWarnings("serial")
public class GmailTag extends SimpleTag {
	public static final GmailTag ALL_MAIL = new GmailTag("ALL", "All mail", "ALL", true);
	public static final GmailTag UNREAD = new GmailTag("UNREAD", "Unread", "UNREAD", true);
	public static final GmailTag INBOX = new GmailTag("INBOX", "Inbox", "INBOX", true);

	private final String path;
	private final boolean hidden;

	public static boolean isHidden(final Label label) {
		return "labelHide".equals(label.getLabelListVisibility())
				|| label.getName().startsWith("CATEGORY_")
				|| label.getId().equals("SENT");
	}

	public static boolean isSystem(final Label label) {
		return "system".equals(label.getType());
	}

	public static String getName(final Label label) {
		String name = label.getName();
		if (name.contains("/") && name.length() > 1) {
			name = name.substring(name.lastIndexOf("/") + 1);
		}
		return name;
	}

	public GmailTag(final Label label) {
		this(label.getId(), getName(label), label.getName(), isHidden(label));
	}

	public GmailTag(final String id, final String name,final String path, final boolean hidden) {
		super(id, name);
		this.path = path;
		this.hidden = hidden;
	}

	public boolean isHidden() {
		return hidden;
	}

	public String getPath() {
		return path;
	}
}
