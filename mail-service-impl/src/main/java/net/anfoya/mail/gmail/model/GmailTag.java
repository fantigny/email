package net.anfoya.mail.gmail.model;

import net.anfoya.tag.model.Tag;

import com.google.api.services.gmail.model.Label;

public class GmailTag extends Tag {
	private final String string;
	private final boolean hidden;

	public GmailTag(final Label label) {
		super(label.getId(), label.getName());

		String string = label.getName();
		if (string.contains("/") && string.length() > 1) {
			string = string.substring(string.lastIndexOf("/") + 1);
		}
		this.string = string;

		this.hidden = "labelHide".equals(label.getLabelListVisibility());
	}

	@Override
	public String toString() {
		return string;
	}

	public boolean isHidden() {
		return hidden;
	}
}
