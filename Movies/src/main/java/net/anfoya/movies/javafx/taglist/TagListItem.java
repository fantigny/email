package net.anfoya.movies.javafx.taglist;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import net.anfoya.movies.model.Tag;

public class TagListItem {
	private final Tag tag;
	private final StringProperty textProperty;
	private final BooleanProperty disableProperty;
	private final BooleanProperty includedProperty;
	private final BooleanProperty excludedProperty;
	private final IntegerProperty movieCountProperty;
	public TagListItem(final Tag tag) {
		this.tag = tag;
		textProperty = new SimpleStringProperty(tag.getName());
		disableProperty = new SimpleBooleanProperty(true);
		movieCountProperty = new SimpleIntegerProperty(0);
		movieCountProperty.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(final ObservableValue<? extends Number> ov, final Number oldVal, final Number newVal) {
				textProperty.set(getTag().getName() + movieCountText());
				if (excludedProperty.get()) {
					disableProperty.set(false);
				} else {
					disableProperty.set(movieCountProperty.get() == 0);
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
	public IntegerProperty movieCountProperty() {
		return movieCountProperty;
	}
	public BooleanProperty includedProperty() {
		return includedProperty;
	}
	public BooleanProperty excludedProperty() {
		return excludedProperty;
	}

	private String movieCountText() {
		if (movieCountProperty.get() == 0) {
			return "";
		}

		return new StringBuilder(" (")
						.append(movieCountProperty.get())
						.append(")")
						.toString();
	}
}