package net.anfoya.javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class IncExcListItem {
	private final StringProperty textProperty;
	private final BooleanProperty disableProperty;
	private final BooleanProperty includedProperty;
	private final BooleanProperty excludedProperty;
	private final IntegerProperty countProperty;
	private final BooleanProperty focusTraversableProperty;

	public IncExcListItem() {
		textProperty = new SimpleStringProperty();
		disableProperty = new SimpleBooleanProperty(true);
		countProperty = new SimpleIntegerProperty(0);
		includedProperty = new SimpleBooleanProperty(false);
		excludedProperty = new SimpleBooleanProperty(false);
		focusTraversableProperty = new SimpleBooleanProperty(true);

		includedProperty.addListener((ov, o, n) -> {
			if (n) {
				excludedProperty.set(false);
			}
		});
		excludedProperty.addListener((ov, o, n) -> {
			if (n) {
				includedProperty.set(false);
				disableProperty.set(false);
			}
		});
	}

	public StringProperty textProperty() {
		return textProperty;
	}
	public BooleanProperty disableProperty() {
		return disableProperty;
	}
	public IntegerProperty countProperty() {
		return countProperty;
	}
	public BooleanProperty includedProperty() {
		return includedProperty;
	}
	public BooleanProperty excludedProperty() {
		return excludedProperty;
	}
	public BooleanProperty focusTraversableProperty() {
		return focusTraversableProperty;
	}
}
