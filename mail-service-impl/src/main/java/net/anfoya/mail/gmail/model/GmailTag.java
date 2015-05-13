package net.anfoya.mail.gmail.model;

import net.anfoya.tag.model.SimpleTag;

import com.google.api.services.gmail.model.Label;

@SuppressWarnings("serial")
public class GmailTag extends SimpleTag {
	private final String path;
	private final boolean hidden;

	public static boolean isHidden(final Label label) {
		return "labelHide".equals(label.getLabelListVisibility())
				|| label.getName().startsWith("CATEGORY_");
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
		super(label.getId(), getName(label));

		this.path = label.getName();
		this.hidden = isHidden(label);
	}

	public boolean isHidden() {
		return hidden;
	}

	public String getPath() {
		return path;
	}
}
