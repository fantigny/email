package net.anfoya.javafx.scene.control;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;

public class ExcludeBox extends CheckBox {

	public ExcludeBox() {
		selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
				if (newVal) {
					setIndeterminate(true);
				}
			}
		});
	}
}
