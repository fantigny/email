package net.anfoya.mail.gmail.model;

import net.anfoya.tag.model.Section;

import com.google.api.services.gmail.model.Label;

public class GmailSection extends Section {
	public static final GmailSection NO_SECTION = new GmailSection(Section.NO_SECTION_NAME);

	private final String string;
	private final boolean hidden;

	private GmailSection(final String name) {
		super(name);

		string = name;
		hidden = false;
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
	}

	@Override
	public String toString() {
		return string;
	}

	public boolean isHidden() {
		return hidden;
	}
}
