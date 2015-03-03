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
	private final StringProperty textProperty = new SimpleStringProperty("");
	private final BooleanProperty disableProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
	private final BooleanProperty excludedProperty = new SimpleBooleanProperty(false);
	private final IntegerProperty movieCountProperty = new SimpleIntegerProperty(0);
	public TagListItem(final Tag tag) {
		this.tag = tag;
		textProperty.set(tag.getName());
		movieCountProperty.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(final ObservableValue<? extends Number> ov, final Number oldVal, final Number newVal) {
				textProperty.set(getTag().getName() + movieCountText());
				if (isExcluded()) {
					disableProperty.set(false);
				} else {
					disableProperty.set(getMovieCount() == 0);
				}
			}
		});
		selectedProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
				if (newVal) {
					excludedProperty.set(false);
				}
			};
		});
		excludedProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
				if (newVal) {
					selectedProperty.set(false);
					disableProperty.set(false);
				}
			};
		});
	}
	public Tag getTag() {
		return tag;
	}
	public StringProperty textProperty() {
		return textProperty;
	}
	public String getText() {
		return textProperty.get();
	}
	public BooleanProperty disableProperty() {
		return disableProperty;
	}
	public boolean isDisable() {
		return disableProperty.get();
	}
	public IntegerProperty movieCountProperty() {
		return movieCountProperty;
	}
	public int getMovieCount() {
		return movieCountProperty.get();
	}
	public BooleanProperty selectedProperty() {
		return selectedProperty;
	}
	public Boolean isSelected() {
		return selectedProperty.get();
	}
	public BooleanProperty excludedProperty() {
		return excludedProperty;
	}
	public Boolean isExcluded() {
		return excludedProperty.get();
	}
	@Override
	public String toString() {
		return textProperty.get();
	}
	private String movieCountText() {
		if (movieCountProperty.intValue() == 0) {
			return "";
		} else {
			return " (" + movieCountProperty.get() + ")";
		}
	}
}