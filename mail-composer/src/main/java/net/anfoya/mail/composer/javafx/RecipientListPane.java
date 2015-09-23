package net.anfoya.mail.composer.javafx;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import net.anfoya.java.util.concurrent.ThreadPool;
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
	private final ComboField comboField;
	private final Set<String> selectedAdresses;

	private Map<String, C> addressContacts;

	private Task<Double> organiseTask;
	private long organiseTaskId;

	private EventHandler<ActionEvent> updateHandler;

	private volatile boolean focusFromPane;

	public RecipientListPane(final String title) {
		super(0);
		setPadding(new Insets(3, 0, 3, 0));
		getStyleClass().add("box-underline");

		this.title = new Label(title);
		this.title.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
		this.title.setStyle("-fx-text-fill: gray; -fx-padding: 2 0 0 0");
		getChildren().add(this.title);

		flowPane = new FlowPane(3,  2);
		flowPane.setMinWidth(150);
		getChildren().add(flowPane);
		HBox.setHgrow(flowPane, Priority.ALWAYS);

		organiseTask = null;
		organiseTaskId = -1;

		addressContacts = new HashMap<String, C>();
		selectedAdresses = new LinkedHashSet<String>();

		comboField = new ComboField();
		comboField.setCellFactory(listView -> {
			return new ListCell<String>() {
				@Override
			    public void updateItem(final String address, final boolean empty) {
			        super.updateItem(address, empty);
			        if (!empty) {
			        	try {
			        		setText(addressContacts.get(address).getFullname() + " (" + addressContacts.get(address).getEmail() + ")");
			        	} catch(final Exception e) {
			        		setText(address);
			        		//TODO: debug anita@glorywell.com.hk
			        	}
			        }
				}
			};
		});
		comboField.setOnAction(e -> addRecipient(comboField.getText()));
		comboField.setOnBackspaceAction(e -> {
			final int lastAddressIndex = flowPane.getChildren().size() - 2;
			if (lastAddressIndex >= 0) {
				remove((Label) flowPane.getChildren().get(lastAddressIndex));
			}
		});
		comboField.focusTraversableProperty().bind(focusTraversableProperty());
		comboField.setFilter(address -> {
			if (selectedAdresses.contains(address)) {
				return "";
			} else {
				final C contact = addressContacts.get(address);
				return contact.getFullname() + " " + contact.getEmail();
			}
		});

		flowPane.getChildren().add(comboField);

		focusFromPane = false;
		focusedProperty().addListener((ov, o, n) -> {
			if (n) {
				focusFromPane = true;
				comboField.requestFocus();
			}
		});
		comboField.focusedProperty().addListener((ov, o, n) -> {
			if (n) {
				if (focusFromPane) {
					focusFromPane = false;
				} else {
					requestFocus();
				}
			}
		});

		heightProperty().addListener((ov, o, n) -> organise(null));
		widthProperty().addListener((ov, o, n) -> organise(null));
	}

	public String getTitle() {
		return title.getText();
	}

	public void setTitle(final String title) {
		this.title.setText(title);
	}

	public void addRecipient(final String address) {
		final String text = addressContacts.containsKey(address)? addressContacts.get(address).getFullname(): address;
		final String tooltip = "remove " + (text.contains("@")? "": "(" + address + ")");
		final RemoveLabel label = new RemoveLabel(text, tooltip);
		label.getStyleClass().add("address-label");

		flowPane.getChildren().add(flowPane.getChildren().size() - 1, label);
		selectedAdresses.add(address);
		updateHandler.handle(null);
		organise(label);

		label.setOnRemove(e -> remove(label, address));
	}

	public Set<String> getRecipients() {
		final Set<String> addresses = new LinkedHashSet<String>(selectedAdresses);
		if (!comboField.getText().isEmpty()) {
			addresses.add(comboField.getText());
		}

		return addresses;
	}

	public void setOnUpdateList(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	public void setAddressContacts(final Map<String, C> addressContacts) {
		if (!this.addressContacts.isEmpty()) {
			throw new RuntimeException("addressContacts can only be set once");
		}
		this.addressContacts = addressContacts;
		comboField.setItems(addressContacts.keySet());
	}

	private void remove(final Label label) {
		String address;
		if (label.getText().contains("@")) {
			address = label.getText();
		} else {
			final String tooltip = label.getTooltip().getText();
			address = tooltip.substring(tooltip.indexOf('('), tooltip.indexOf(')'));
		}
		remove(label, address);
	}

	private void remove(final Label label, final String address) {
		flowPane.getChildren().remove(label);
		selectedAdresses.remove(address);
		updateHandler.handle(null);
		organise(null);
	}

	private synchronized void organise(final Label lastAdded) {
		final long taskId = ++organiseTaskId;
		if (organiseTask != null && organiseTask.isRunning()) {
			organiseTask.cancel();
		}

		final double comboWidth = comboField.getWidth();
		LOGGER.debug("combo width {}", comboWidth);

		comboField.setText("");

		if (lastAdded != null) {
			final double tempWidth = comboWidth - LabelHelper.computeWidth(lastAdded) - flowPane.getHgap();
			LOGGER.debug("combo temp width {}", comboWidth);
			comboField.setPrefWidth(tempWidth);
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
			comboField.setPrefWidth(availableWidth < 150? flowPane.getWidth(): availableWidth);
		});
		ThreadPool.getInstance().submitHigh(organiseTask);
	}
}
