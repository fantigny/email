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
	private final ListView<String> listView;
	private final ObservableList<String> items;
	private final BooleanProperty showingProperty;

	private volatile boolean backspaceEventReady;
	private Predicate<String> userFilter;

	private EventHandler<ActionEvent> backspaceHandler;
	
	public ComboField2() {
		super("");
		this.items = FXCollections.observableArrayList();
		
		listView = new ListView<String>();
		popup = new Popup();
		popup.getContent().add(listView);
		showingProperty = new SimpleBooleanProperty(false);
		
		userFilter = null;
		backspaceEventReady = true;
		
		setOnKeyPressed(e -> {
			if (KeyCode.BACK_SPACE == e.getCode() && backspaceEventReady) {
				backspaceHandler.handle(null);
			}
			backspaceEventReady = KeyCode.BACK_SPACE == e.getCode();
		});
		
		textProperty().addListener((ov, o, n) -> {
			LOGGER.debug("handle text changed from \"{}\" to \"{}\"", o, n);

			if (n.isEmpty()) {
				// text is empty
				LOGGER.debug("text is empty", n);
				showingProperty.set(false);
				return;
			}

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
				listView.setItems(task.getValue());
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
		});
		
		focusedProperty().addListener((ov, o, n) -> {
			if (o && !n && isShowing()) {
				hide();
			}
		});
		
		showingProperty().addListener((ov, o, n) -> {
			if (n) {
				popup.show(this, ComboField2.this.getLayoutBounds().getMinX() + ComboField2.this.getLayoutX()
						, ComboField2.this.getLayoutBounds().getMinY() + ComboField2.this.getLayoutY() + ComboField2.this.getHeight());
			} else {
				popup.hide();
			}
		});
	}

	public void setFilter(Predicate<String> userFilter) {
		this.userFilter = userFilter;
	}
	
	public void setItems(Set<String> items) {
		this.items.setAll(items);
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

	public void setCellFactory(Callback<ListView<String>, ListCell<String>> factory) {
		listView.setCellFactory(factory);
	}

	public void setOnBackspaceAction(EventHandler<ActionEvent> handler) {
		backspaceHandler = handler;
	}
}
