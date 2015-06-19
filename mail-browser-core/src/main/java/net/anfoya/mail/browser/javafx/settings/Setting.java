package net.anfoya.mail.browser.javafx.settings;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public enum Setting {
	INSTANCE;

	private BooleanProperty showToolbar;

	private Setting() {
		showToolbar = new SimpleBooleanProperty(false);
	}

	public BooleanProperty showToolbar() {
		return showToolbar;
	}
}
