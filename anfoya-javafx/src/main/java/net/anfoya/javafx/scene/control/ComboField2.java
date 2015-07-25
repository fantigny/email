package net.anfoya.javafx.scene.control;

import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import javafx.util.Callback;
import net.anfoya.java.lang.StringHelper;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.Utils;

public class ComboField2 extends TextField {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComboField2.class);

	private final Popup popup;
	private final ObservableList<String> items;
	private final BooleanProperty showingProperty;

	private final ListView<String> listView;

	private Callback<String, String> textFactory;
	private Task<ObservableList<String>> filterTask;
	private int filterTaskId;

	private volatile boolean emptyBackspaceReady;
	private EventHandler<ActionEvent> backspaceHandler;

	public ComboField2() {
		super("");
        getStyleClass().add("combo-noarrow");
		this.items = FXCollections.observableArrayList();

		listView = new ListView<String>();
		listView.setOnMouseClicked(e -> {
			if (!listView.getSelectionModel().isEmpty()) {
				actionFromListView();
			}
		});
		listView.setOnKeyPressed(e -> {
			if (KeyCode.ENTER == e.getCode()) {
				if (!listView.getSelectionModel().isEmpty()) {
					actionFromListView();
				} else if (!getText().isEmpty()) {
					actionFromTextField();
				}
			}
		});

		popup = new Popup();
		popup.getContent().add(listView);
		showingProperty = new SimpleBooleanProperty(false);

		setContextMenu(new ContextMenu(new MenuItem("", listView)));

		textFactory = null;

		emptyBackspaceReady = true;
		setOnKeyPressed(e -> {
			if (KeyCode.BACK_SPACE == e.getCode()
					&& emptyBackspaceReady
					&& backspaceHandler != null) {
				backspaceHandler.handle(new ActionEvent());
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
				final Point2D p = Utils.pointRelativeTo(ComboField2.this,
		                prefWidth(-1), prefHeight(-1),
		                HPos.CENTER, VPos.BOTTOM, 0, 0, true);
				popup.show(this, p.getX(), p.getY());
			} else {
				popup.hide();
			}
		});

		popup.setOnShown(e -> {
		});
	}

	public void setFilter(final Callback<String, String> textFactory) {
		this.textFactory = textFactory;
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

	private synchronized void filter(final String n) {
		final long taskId = ++filterTaskId;
		if (filterTask != null && filterTask.isRunning()) {
			filterTask.cancel();
		}

		if (n.isEmpty()) {
			hide();
			LOGGER.debug("cancel filter, text is empty", n);
			return;
		}

		filterTask = new Task<ObservableList<String>>() {
			@Override
			protected ObservableList<String> call() throws Exception {
				return FXCollections.observableArrayList(items.filtered(s ->
					textFactory == null
						? StringHelper.containsIgnoreCase(s, n)
						: StringHelper.containsIgnoreCase(textFactory.call(s), n)));
			}
		};
		filterTask.setOnSucceeded(e -> {
			if (taskId != filterTaskId) {
				return;
			}
			final ObservableList<String> filtered = filterTask.getValue();
			if (filtered.isEmpty()) {
				hide();
				LOGGER.debug("no item match");
			} else {
				updatePopup(filtered);
				show();
				LOGGER.debug("filtered {} item(s)", listView.getItems().size());
			}
		});
		filterTask.setOnFailed(e -> LOGGER.error("filtering items with \"{}\"", n, e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(filterTask);
	}

	private void updatePopup(final ObservableList<String> items) {
		final double height = 25 * Math.min(10, Math.max(2, items.size())); //TODO get from cell

		listView.getItems().setAll(items);
		listView.setPrefHeight(height);
		listView.setMinHeight(height);
		listView.setMaxHeight(height);
		listView.setMinWidth(500); //TODO get from cell
		listView.setMaxWidth(500);
	}

	private void actionFromTextField() {
		LOGGER.warn("action from textfield, text: {}", getText());
		getOnAction().handle(new ActionEvent());
	}

	private void actionFromListView() {
		LOGGER.warn("action from listview, selected item: {}", listView.getSelectionModel().getSelectedItem());
		setText(listView.getSelectionModel().getSelectedItem());
		getOnAction().handle(new ActionEvent());
		hide();
	}
}
