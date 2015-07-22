package net.anfoya.mail.composer.javafx;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.AutoShowComboBoxHelper;
import net.anfoya.javafx.scene.control.ComboField;
import net.anfoya.javafx.scene.control.RemoveLabel;
import net.anfoya.javafx.util.LabelHelper;
import net.anfoya.mail.service.Contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipientListPane<C extends Contact> extends HBox {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecipientListPane.class);

	private final Label title;
	private final FlowPane flowPane;
	private final ComboField<String> combo;
	private final Map<String, C> addressContacts;
	private final Set<String> selectedAdresses;

	private Task<Double> organiseTask;
	private long organiseTaskId;

	private EventHandler<ActionEvent> updateHandler;

	public RecipientListPane(final String title, final Set<C> contacts) {
		super(0);
		setAlignment(Pos.CENTER_LEFT);
		getStyleClass().add("box-underline");

		addressContacts = new LinkedHashMap<String, C>();
		for(final C c: contacts) {
			addressContacts.put(c.getEmail(), c);
		}

		this.title = new Label(title);
		this.title.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
		this.title.setStyle("-fx-text-fill: gray");
		getChildren().add(this.title);

		flowPane = new FlowPane(3,  2);
		flowPane.setMinWidth(150);
		getChildren().add(flowPane);
		HBox.setHgrow(flowPane, Priority.ALWAYS);

		organiseTask = null;
		organiseTaskId = -1;

		selectedAdresses = new LinkedHashSet<String>();

		combo = new ComboField<String>();
		combo.setPadding(new Insets(0));
		combo.getItems().addAll(addressContacts.keySet());
		combo.setOnFieldAction(e -> add(combo.getFieldValue()));
		combo.setCellFactory(listView -> {
			return new ListCell<String>() {
				@Override
			    public void updateItem(final String address, final boolean empty) {
			        super.updateItem(address, empty);
			        if (!empty) {
			        	setText(addressContacts.get(address).getFullname() + " (" + addressContacts.get(address).getEmail() + ")");
			        }
				}
			};
		});
		combo.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
			if (combo.getValue() == null
					|| combo.getValue().isEmpty()
					&& e.getCode() == KeyCode.BACK_SPACE
					&& flowPane.getChildren().size() > 1) {
				flowPane.getChildren().remove(flowPane.getChildren().size()-2);
			}
		});
		new AutoShowComboBoxHelper(combo, address -> selectedAdresses.contains(address)
				? ""
				: addressContacts.get(address).getFullname() + " " + addressContacts.get(address).getEmail());

		flowPane.getChildren().add(combo);
		heightProperty().addListener((ov, o, n) -> organise(null));
		widthProperty().addListener((ov, o, n) -> organise(null));
	}

	public void add(final String address) {
		final RemoveLabel label = new RemoveLabel(addressContacts.get(address).getFullname(), address);
		label.getStyleClass().add("address-label");

		flowPane.getChildren().add(flowPane.getChildren().size() - 1, label);
		selectedAdresses.add(address);
		updateHandler.handle(null);
		organise(label);

		label.setOnRemove(e -> {
			flowPane.getChildren().remove(label);
			selectedAdresses.remove(label.getUserId());
			updateHandler.handle(null);
			organise(null);
		});
	}

	public Set<Address> getRecipients() {
		final Set<Address> addresses = new LinkedHashSet<Address>();
		for(final String address: selectedAdresses) {
			try {
				addresses.add(new InternetAddress(address));
			} catch (final AddressException e) {
				LOGGER.error("error parsing address {}", address);
			}
		}
		if (combo.getValue() != null && !combo.getValue().isEmpty()) {
			try {
				addresses.add(new InternetAddress(combo.getValue()));
			} catch (final AddressException e) {
				LOGGER.error("error parsing address {}", combo.getValue());
			}
		}

		return addresses;
	}

	public void setOnUpdateList(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	public ReadOnlyBooleanProperty textfocusedProperty() {
		return combo.getEditor().focusedProperty();
	}

	public String getTitle() {
		return title.getText();
	}

	public void setTitle(final String title) {
		this.title.setText(title);
	}

	private synchronized void organise(final Label lastAdded) {
		final long taskId = ++organiseTaskId;
		if (organiseTask != null && organiseTask.isRunning()) {
			organiseTask.cancel();
		}

		final double comboWidth = combo.getWidth();
		LOGGER.debug("combo width {}", comboWidth);

		combo.setFieldValue("");
		combo.hide();

		if (lastAdded != null) {
			final double tempWidth = comboWidth - LabelHelper.computeWidth(lastAdded) - flowPane.getHgap();
			LOGGER.debug("combo temp width {}", comboWidth);
			combo.setPrefWidth(tempWidth);
		}

		organiseTask = new Task<Double>() {
			@Override
			protected Double call() throws Exception {
				final double paneWidth = flowPane.getWidth();
				LOGGER.debug("pane width {}", paneWidth);
				if (paneWidth == 0) {
					return 0d;
				}

				double availableWidth = paneWidth;
				for(final Node node: flowPane.getChildren()) {
					if (node instanceof Label) {
						final Label l = (Label) node;
						while(l.getWidth() == 0) {
							try { Thread.sleep(50); }
							catch (final Exception e) { /* do nothing */ }
						}
						if (l.getWidth() > availableWidth) {
							availableWidth = paneWidth;
						}
						availableWidth -= l.getWidth() + flowPane.getHgap();
					}
				}
				LOGGER.debug("available width {}", availableWidth);
				return availableWidth;
			}
		};
		organiseTask.setOnFailed(event -> LOGGER.error("organizing labels and combo", event.getSource().getException()));
		organiseTask.setOnSucceeded(e -> {
			if (taskId != organiseTaskId) {
				return;
			}

			final double availableWidth = (double) e.getSource().getValue();
			combo.setPrefWidth(availableWidth < 150? flowPane.getWidth(): availableWidth);
		});
		ThreadPool.getInstance().submitHigh(organiseTask);
	}
}
