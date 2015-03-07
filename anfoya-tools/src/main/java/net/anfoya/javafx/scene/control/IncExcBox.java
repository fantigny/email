package net.anfoya.javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;

public class IncExcBox extends CheckBox {
	private final ExcludeBox excludeBox;
	private final BooleanProperty includedProperty;

	public IncExcBox() {
		excludeBox = new ExcludeBox();

		setAllowIndeterminate(false);
		setGraphic(new BorderPane(getGraphic(), null, excludeBox, null, null));

		includedProperty = new SimpleBooleanProperty(false);
		includedProperty.bindBidirectional(selectedProperty());

		includedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
				if (newVal) {
					setExcluded(false);
				}
			}
		});

		excludeBox.excludedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
				if (newVal) {
					setIncluded(false);
				}
			}
		});
	}
	public BooleanProperty includedProperty() {
		return includedProperty;
	}
	public boolean isIncluded() {
		return includedProperty.get();
	}
	public void setIncluded(final boolean value) {
		includedProperty.set(value);
	}
	public BooleanProperty excludedProperty() {
		return excludeBox.excludedProperty();
	}
	public boolean isExcluded() {
		return excludeBox.isExcluded();
	}
	public void setExcluded(final boolean value) {
		excludeBox.setExcluded(value);
	}
}
