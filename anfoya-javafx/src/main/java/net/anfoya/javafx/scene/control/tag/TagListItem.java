package net.anfoya.javafx.scene.control.tag;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import net.anfoya.javafx.scene.control.tag.model.Tag;

public class TagListItem {
	private static final String COUNT_STRING = " (%d)";

	private final Tag tag;
	private final StringProperty textProperty;
	private final BooleanProperty disableProperty;
	private final BooleanProperty includedProperty;
	private final BooleanProperty excludedProperty;
	private final IntegerProperty countProperty;
	public TagListItem(final Tag tag) {
		this.tag = tag;
		textProperty = new SimpleStringProperty(tag.getName());
		disableProperty = new SimpleBooleanProperty(true);
		countProperty = new SimpleIntegerProperty(0);
		countProperty.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(final ObservableValue<? extends Number> ov, final Number oldVal, final Number newVal) {
				textProperty.set(getTag().getName() + getCountAsString());
				if (excludedProperty.get()) {
					disableProperty.set(false);
				} else {
					disableProperty.set(countProperty.get() == 0);
				}
			}
		});
		includedProperty = new SimpleBooleanProperty(false);
		includedProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
				if (newVal) {
					excludedProperty.set(false);
				}
			};
		});
		excludedProperty = new SimpleBooleanProperty(false);
		excludedProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
				if (newVal) {
					includedProperty.set(false);
					disableProperty.set(false);
				}
			};
		});
	}

	@Override
	public String toString() {
		return textProperty.get();
	}

	public Tag getTag() {
		return tag;
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

	private String getCountAsString() {
		if (countProperty.get() == 0) {
			// don't display null count
			return "";
		}

		return countProperty.get() == 0? "": String.format(COUNT_STRING, countProperty.get());
	}
}