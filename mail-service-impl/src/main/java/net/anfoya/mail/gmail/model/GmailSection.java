package net.anfoya.mail.gmail.model;

import net.anfoya.tag.model.SimpleSection;

import com.google.api.services.gmail.model.Label;

@SuppressWarnings("serial")
public class GmailSection extends SimpleSection {
	public static final GmailSection TO_SORT = new GmailSection(SimpleSection.NO_SECTION_NAME);
	public static final GmailSection SYSTEM = new GmailSection("GMail");

	private final String string;
	private final boolean hidden;
	private final String path;

	private GmailSection(final String name) {
		super(name);

		string = name;
		hidden = false;
		path = name;
	}

	public GmailSection(final Label label) {
		super(label.getId(), label.getName());

		final String name = label.getName();
		if (name.contains("/")) {
			string = name.substring(0, name.lastIndexOf("/"));
		} else {
			string = name;
		}

		hidden = "labelHide".equals(label.getLabelListVisibility());
		path = label.getName();
	}

	@Override
	public String toString() {
		return string;
	}

	public boolean isHidden() {
		return hidden;
	}

	public String getPath() {
		return path;
	}
}
