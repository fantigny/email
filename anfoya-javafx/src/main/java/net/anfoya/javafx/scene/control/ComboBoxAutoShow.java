package net.anfoya.javafx.scene.control;

import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.util.Callback;
import net.anfoya.java.lang.StringHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComboBoxAutoShow {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComboBoxAutoShow.class);

	public ComboBoxAutoShow(final ComboBox<String> comboBox, final Callback<String, String> textBuilder) {
		final ObservableList<String> items = FXCollections.observableArrayList(comboBox.getItems());

		comboBox.getEditor().textProperty().addListener((ov, o, n) -> {
			LOGGER.debug("handle text changed from \"{}\" to \"{}\"", o, n);

			if (n.isEmpty()) {
				// text is empty
				LOGGER.debug("text is empty", n);
				comboBox.hide();
				return;
			}

			if (n.equals(comboBox.getSelectionModel().getSelectedItem())) {
				// user is going through the list
				LOGGER.debug("text changed to selection \"{}\"", n);
				return;
			}

			comboBox.hide();

			final Predicate<String> filter = s -> {
				final String src = textBuilder.call(s);
				return StringHelper.containsIgnoreCase(src, n);
			};
			final FilteredList<String> filtered = items.filtered(filter);
			if (filtered.isEmpty()) {
				LOGGER.debug("no item match");
				comboBox.getItems().setAll(items);
			} else {
				LOGGER.debug("filtered {} item(s)", filtered.size());
				comboBox.getItems().setAll(filtered);
				comboBox.show();
			}
		});
	}
}