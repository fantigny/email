package net.anfoya.javafx.scene.control;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import javafx.util.Callback;
import net.anfoya.java.lang.StringHelper;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.java.util.concurrent.ThreadPool;

public class ComboField extends TextField {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComboField.class);

	private final ObservableList<String> items;

	private final Popup popup;
	private final ListView<String> listView;
	private final BooleanProperty showingProperty;
	private volatile boolean firstShow;

	private Callback<String, String> textFactory;
	private Task<ObservableList<String>> filterTask;
	private int filterTaskId;

	private volatile boolean emptyBackspaceReady;
	private EventHandler<ActionEvent> backspaceHandler;

	private double cellHeight;
	private double cellWidth;

	private Callback<ListView<String>, ListCell<String>> cellFactory;

	public ComboField() {
		super("");
        getStyleClass().add("combofield");
		this.items = FXCollections.observableArrayList();

		listView = new ListView<String>();
		listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
		    @Override public ListCell<String> call(final ListView<String> listView) {
		    	final ListCell<String> cell = cellFactory  == null? new ListCell<String>(): cellFactory.call(listView);
		    	if (firstShow) {
			    	cell.widthProperty().addListener((ov, o, n) -> {
						updateCellSize(cell.getHeight(), cell.getWidth());
					});
		    	}
		    	return cell;
		    }
		});
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

		firstShow = true;
		popup.setOnShown(e -> firstShow = false);

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
				final Point2D pos = localToScene(0, 0);
				popup.show(this
						, getScene().getWindow().getX() + getScene().getX() + pos.getX()
						, getScene().getWindow().getY() + getScene().getY() + pos.getY() + getBoundsInParent().getHeight());
			} else {
				popup.hide();
			}
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
		cellFactory =  factory;
	}

	public void setCellSize(final double height, final double width) {
		cellHeight = height;
		cellWidth = width;
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
		filterTask.setOnFailed(e -> LOGGER.error("filtering items with {}", n, e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "filtering items with " + n, filterTask);
	}

	private void updatePopup(final ObservableList<String> items) {
		final double height = Math.max(25, Math.min(200, items.size() * ((cellHeight > 0? cellHeight:24) + 1)));
		final double width = Math.max(100, Math.min(500, cellWidth > 0? cellWidth: 500));

		LOGGER.debug("height {} width {}", height, width);
		listView.setMaxHeight(height);
		listView.setMinHeight(height);
		listView.setPrefHeight(height);
		popup.setHeight(height);
		listView.setMaxWidth(width);
		listView.setMinWidth(width);
		listView.setPrefWidth(width);
		popup.setWidth(width);

		listView.getItems().setAll(items);
	}

	private void updateCellSize(final double height, final double width) {
		if (!firstShow) {
			return;
		}
		if (cellHeight < height) {
			cellHeight = height;
			LOGGER.debug("update cell height {}", height);
		}
		if (cellWidth <=0 && cellWidth < width) {
			cellWidth = width;
			LOGGER.debug("update cell width {}", width);
		}
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
