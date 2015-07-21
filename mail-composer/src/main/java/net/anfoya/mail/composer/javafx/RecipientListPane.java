package net.anfoya.mail.composer.javafx;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.AutoShowComboBoxHelper;
import net.anfoya.javafx.scene.control.ComboField;
import net.anfoya.javafx.util.LabelHelper;
import net.anfoya.mail.service.Contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipientListPane<C extends Contact> extends FlowPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecipientListPane.class);
	private static final String RECIPIENT_LABEL_SUFFIX = " X";

	private final ComboField<String> combo;
	private final Map<String, C> addressContacts;
	private final Set<String> selectedAdresses;

	private Task<Double> organiseTask;
	private long organiseTaskId;

	private EventHandler<ActionEvent> updateHandler;

	public RecipientListPane(final Set<C> contacts) {
		super(3, 2);
		setMinWidth(150);

		organiseTask = null;
		organiseTaskId = -1;

		addressContacts = new LinkedHashMap<String, C>();
		for(final C c: contacts) {
			addressContacts.put(c.getEmail(), c);
		}

		selectedAdresses = new LinkedHashSet<String>();

		combo = new ComboField<String>();
		combo.getItems().addAll(addressContacts.keySet());
		combo.setOnFieldAction(e -> add(combo.getFieldValue()));
		combo.setCellFactory(listView -> {
			return new ListCell<String>() {
				@Override
			    public void updateItem(final String address, final boolean empty) {
			        super.updateItem(address, empty);
			        if (!empty) {
			        	setText(createRecipientText(address));
			        }
				}
			};
		});
		new AutoShowComboBoxHelper(combo, address -> selectedAdresses.contains(address)? "": createRecipientText(address));

		getChildren().add(combo);
		heightProperty().addListener((ov, o, n) -> organise(null));
		widthProperty().addListener((ov, o, n) -> organise(null));
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

	public void add(final String address) {
		final Label label = createRecipientLabel(address);
		getChildren().add(getChildren().size() - 1, label);
		selectedAdresses.add(address);
		updateHandler.handle(null);
		organise(label);
	}

	private void remove(final Label label) {
		getChildren().remove(label);
		selectedAdresses.remove(getRecipientAddress(label));
		updateHandler.handle(null);
		organise(null);
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
			final double tempWidth = comboWidth - LabelHelper.computeWidth(lastAdded) - getHgap();
			LOGGER.debug("combo temp width {}", comboWidth);
			combo.setPrefWidth(tempWidth);
		}

		organiseTask = new Task<Double>() {
			@Override
			protected Double call() throws Exception {
				final double paneWidth = getWidth();
				LOGGER.debug("pane width {}", paneWidth);
				if (paneWidth == 0) {
					return 0d;
				}

				double availableWidth = paneWidth;
				for(final Node node: getChildren()) {
					if (node instanceof Label) {
						final Label l = (Label) node;
						while(l.getWidth() == 0) {
							try { Thread.sleep(50); }
							catch (final Exception e) { /* do nothing */ }
						}
						if (l.getWidth() > availableWidth) {
							availableWidth = paneWidth;
						}
						availableWidth -= l.getWidth() + getHgap();
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
			combo.setPrefWidth(availableWidth < 150? getWidth(): availableWidth);
		});
		ThreadPool.getInstance().submitHigh(organiseTask);
	}

	private String createRecipientText(final String address) {
		final C contact = addressContacts.get(address);
		if (contact == null) {
			return address;
		} else {
			return contact.getFullname() + " (" + contact.getEmail() + ")";
		}
	}

	private Label createRecipientLabel(final String address) {
		final String text = createRecipientText(address);

		final Label label = new Label(text + RECIPIENT_LABEL_SUFFIX);
		label.getStyleClass().add("address-label");
		label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
		label.setOnMouseClicked(e -> remove(label));

		return label;
	}

	private String getRecipientAddress(final Label label) {
		final String text = label.getText();
		String address;
		if (text.contains("(")) {
			address = text.substring(text.indexOf("(") + 1, text.indexOf(")"));
		} else {
			address = text.substring(0, text.length() - RECIPIENT_LABEL_SUFFIX.length());
		}
		return address;
	}

	public void setOnUpdateList(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	public ReadOnlyBooleanProperty textfocusedProperty() {
		return combo.getEditor().focusedProperty();
	}
}
