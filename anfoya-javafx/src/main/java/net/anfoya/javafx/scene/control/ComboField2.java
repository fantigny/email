package net.anfoya.javafx.scene.control;

import java.util.Set;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import javafx.util.Callback;
import net.anfoya.java.lang.StringHelper;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComboField2 extends TextField {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComboField2.class);

	private final Popup popup;
	private final ObservableList<String> items;
	private final BooleanProperty showingProperty;

	private volatile boolean emptyBackspaceReady;
	private Predicate<String> userFilter;

	private EventHandler<ActionEvent> backspaceHandler;

	private final ListView<String> listView;

	public ComboField2() {
		super("");
		this.items = FXCollections.observableArrayList();

		listView = new ListView<String>();
		popup = new Popup();
		popup.setAutoFix(true);
		popup.setAutoHide(false);
		popup.getContent().add(listView);
		showingProperty = new SimpleBooleanProperty(false);

		userFilter = null;
		emptyBackspaceReady = true;

		setOnKeyPressed(e -> {
			if (KeyCode.BACK_SPACE == e.getCode() && emptyBackspaceReady) {
				backspaceHandler.handle(null);
			}
		});

		textProperty().addListener((ov, o, n) -> {
			emptyBackspaceReady = n.isEmpty();
			filter(n);
		});

		focusedProperty().addListener((ov, o, n) -> {
			if (o && !n && isShowing()) {
				hide();
			}
		});

		showingProperty().addListener((ov, o, n) -> {
			if (n) {
				final Point2D p = listView.localToScene(0.0, 0.0);
				popup.show(this,
				        p.getX() + listView.getScene().getWindow().getX(),
				        p.getY() + listView.getScene().getWindow().getY());
			} else {
				popup.hide();
			}
		});
	}

	private void filter(final String n) {
		LOGGER.debug("filter with text \"{}\"", n);
		final Task<ObservableList<String>> task = new Task<ObservableList<String>>() {
			@Override
			protected ObservableList<String> call() throws Exception {
				return FXCollections.observableArrayList(ComboField2.this.items.filtered(s ->
					userFilter == null
						? StringHelper.containsIgnoreCase(s, n)
						: userFilter.test(s)));
			}
		};
		task.setOnSucceeded(e -> {
			listView.getItems().setAll(task.getValue());
			if (listView.getItems().isEmpty()) {
				hide();
				LOGGER.debug("no item match");
			} else {
				show();
				LOGGER.debug("filtered {} item(s)", listView.getItems().size());
			}
		});
		task.setOnFailed(e -> LOGGER.error("filtering items with \"{}\"", n, e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(task);
	}

	public void setFilter(final Predicate<String> userFilter) {
		this.userFilter = userFilter;
	}

	public void setItems(final Set<String> items) {
		this.items.setAll(items);
		if (isShowing()) {
			filter(getText());
		}
	}

	public boolean isShowing() {
		return showingProperty.get();
	}

	public BooleanProperty showingProperty() {
		return showingProperty;
	}

	public void show() {
		showingProperty.set(true);
	}

	public void hide() {
		showingProperty.set(false);
	}

	public void setCellFactory(final Callback<ListView<String>, ListCell<String>> factory) {
		listView.setCellFactory(factory);
	}

	public void setOnBackspaceAction(final EventHandler<ActionEvent> handler) {
		backspaceHandler = handler;
	}
}
