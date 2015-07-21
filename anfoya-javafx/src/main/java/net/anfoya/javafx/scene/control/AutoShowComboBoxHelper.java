package net.anfoya.javafx.scene.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.util.Callback;
import net.anfoya.java.lang.StringHelper;

public class AutoShowComboBoxHelper {
	public AutoShowComboBoxHelper(final ComboBox<String> comboBox, final Callback<String, String> textBuilder) {
		final ObservableList<String> items = FXCollections.observableArrayList(comboBox.getItems());

		comboBox.getEditor().textProperty().addListener((ov, o, n) -> {
			if (n.equals(comboBox.getSelectionModel().getSelectedItem())) {
				// user is going through the list
				return;
			}

			comboBox.hide();
			final FilteredList<String> filtered = items.filtered(s -> StringHelper.containsIgnoreCase(textBuilder.call(s), n));
			if (filtered.isEmpty()) {
				comboBox.getItems().setAll(items);
			} else {
				comboBox.getItems().setAll(filtered);
				comboBox.show();
			}
		});
	}
}