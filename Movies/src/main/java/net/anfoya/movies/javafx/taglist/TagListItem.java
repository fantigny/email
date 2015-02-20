package net.anfoya.movies.javafx.taglist;

import net.anfoya.movies.model.Tag;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class TagListItem {
	private final Tag tag;
	private final StringProperty textProperty = new SimpleStringProperty("");
	private final BooleanProperty disableProperty = new SimpleBooleanProperty(true);
	private final IntegerProperty movieCountProperty = new SimpleIntegerProperty(0);
	private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
	public TagListItem(final Tag tag) {
		this.tag = tag;
		textProperty.set(tag.getName());
		movieCountProperty.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(final ObservableValue<? extends Number> ov, final Number oldVal, final Number newVal) {
				textProperty.set(getTag().getName() + movieCountText());
				disableProperty.set(getMovieCount() == 0);
			}
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