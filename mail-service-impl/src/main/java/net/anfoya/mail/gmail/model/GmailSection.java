package net.anfoya.mail.gmail.model;

import net.anfoya.tag.model.SimpleSection;

import com.google.api.services.gmail.model.Label;

@SuppressWarnings("serial")
public class GmailSection extends SimpleSection {
	public static final GmailSection SYSTEM = new GmailSection("GMail");

	private final String string;
	private final String path;

	private GmailSection(final String name) {
		super(name);

		string = name;
		path = name;
	}

	public GmailSection(final Label label) {
		super(label.getId(), label.getName(), false);

		final String name = label.getName();
		if (name.contains("/")) {
			string = name.substring(0, name.lastIndexOf("/"));
		} else {
			string = name;
		}

		path = label.getName();
	}

	@Override
	public String toString() {
		return string;
	}

	public String getPath() {
		return path;
	}

	public static boolean isHidden(final Label label) {
		return "labelHide".equals(label.getLabelListVisibility());
	}
}
