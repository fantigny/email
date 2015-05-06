package net.anfoya.mail.gmail.model;

import net.anfoya.tag.model.SimpleTag;

import com.google.api.services.gmail.model.Label;

public class GmailTag extends SimpleTag {
	private final String path;
	private final boolean hidden;

	private static String getTagName(final Label label) {
		String name = label.getName();
		if (name.contains("/") && name.length() > 1) {
			name = name.substring(name.lastIndexOf("/") + 1);
		}
		return name;
	}

	public GmailTag(final Label label) {
		super(label.getId(), getTagName(label));

		this.path = label.getName();
		this.hidden = "labelHide".equals(label.getLabelListVisibility());
	}

	public boolean isHidden() {
		return hidden;
	}

	public String getPath() {
		return path;
	}
}
