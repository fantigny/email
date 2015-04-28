package net.anfoya.javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;

public class ExcludeBox extends CheckBox {
    private static final String DEFAULT_STYLE_CLASS = "exclude-box";

	private final BooleanProperty excludedProperty;
	public ExcludeBox() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
		setAllowIndeterminate(false);
		excludedProperty = new SimpleBooleanProperty();
		excludedProperty.bindBidirectional(selectedProperty());
	}

	public BooleanProperty excludedProperty() {
		return excludedProperty;
	}
	public boolean isExcluded() {
		return excludedProperty.get();
	}
	public void setExcluded(final boolean value) {
		excludedProperty.set(value);
	}
}
