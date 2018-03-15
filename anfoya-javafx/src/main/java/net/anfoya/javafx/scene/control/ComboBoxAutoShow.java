package net.anfoya.javafx.scene.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import javafx.util.Callback;
import net.anfoya.java.lang.StringHelper;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.java.util.concurrent.ThreadPool;

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

			final Task<FilteredList<String>> task = new Task<FilteredList<String>>() {
				@Override
				protected FilteredList<String> call() throws Exception {
					return items.filtered(s -> {
						final String src = textBuilder.call(s);
						return StringHelper.containsIgnoreCase(src, n);
					});
				}
			};
			task.setOnSucceeded(e -> {
				comboBox.hide();
				comboBox.setItems(FXCollections.observableArrayList(task.getValue()));
				if (comboBox.getItems().isEmpty()) {
					LOGGER.debug("no item match");
				} else {
					LOGGER.debug("filtered {} item(s)", comboBox.getItems().size());
					comboBox.show();
				}
			});
			task.setOnFailed(e -> LOGGER.error("filtering items with {}", n, e.getSource().getException()));
			ThreadPool.getDefault().submit(PoolPriority.MAX, "filtering items with " + n, task);
		});
	}
}