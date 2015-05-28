package net.anfoya.javafx.scene.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.util.Callback;


public class AutoCompComboBoxListener {
    private final ComboBox<String> comboBox;
	private final ObservableList<String> items;
	private final Callback<String, String> callback;

    public AutoCompComboBoxListener(final ComboBox<String> toCombo, final Callback<String, String> callback) {
        this.comboBox = toCombo;
        this.callback = callback;

        items = FXCollections.observableArrayList(toCombo.getItems());

        this.comboBox.setEditable(true);
        toCombo.getEditor().textProperty().addListener((ov, oldVal, newVal) -> keyReleased(oldVal, newVal));
    }

    public void keyReleased(final String oldVal, final String newVal) {
    	if (items.contains(newVal)) {
    		return;
    	}

		final FilteredList<String> filteredList = new FilteredList<String>(items, s -> callback.call(s).startsWith(newVal));
        comboBox.getItems().clear();
        comboBox.getItems().setAll(filteredList);

        if (!comboBox.isShowing()) {
        	comboBox.show();
        }
    }
}