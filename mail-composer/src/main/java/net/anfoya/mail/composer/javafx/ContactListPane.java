package net.anfoya.mail.composer.javafx;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.AutoShowComboBoxListener;
import net.anfoya.javafx.scene.control.ComboField;
import net.anfoya.javafx.util.LabelHelper;
import net.anfoya.mail.service.Contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContactListPane<C extends Contact> extends FlowPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ContactListPane.class);

	private final ComboField<String> combo;

	private Task<Double> organiseTask;
	private long organiseTaskId;

	public ContactListPane(final Set<C> contacts) {
		super(3, 2);
		setMinWidth(150);

		organiseTask = null;
		organiseTaskId = -1;

		final Map<String, C> emailContacts = new LinkedHashMap<String, C>();
		for(final C c: contacts) {
			emailContacts.put(c.getEmail(), c);
		}

		final Callback<String, String> contactDisplay = email -> {
			return emailContacts.get(email).getFullname() + " <" + email + ">";
		};

		combo = new ComboField<String>();
		combo.getItems().addAll(emailContacts.keySet());
		combo.setOnFieldAction(e -> add(combo.getFieldValue()));
		combo.setCellFactory(listView -> {
			return new ListCell<String>() {
				@Override
			    public void updateItem(final String email, final boolean empty) {
			        super.updateItem(email, empty);
			        if (!empty) {
			        	final String display = contactDisplay.call(email);
			        	setText(display);
			        }
				}
			};
		});
		new AutoShowComboBoxListener(combo, email -> isSelected(email)? "": contactDisplay.call(email));
		organise();

		getChildren().add(combo);
		heightProperty().addListener((ov, o, n) -> organise());
		widthProperty().addListener((ov, o, n) -> organise());
	}

	public void add(final String contactEmail) {
		if (contactEmail == null || contactEmail.isEmpty()) {
			return;
		}
		final Label label = new Label(getLabel(contactEmail));
		label.getStyleClass().add("address-label");
		label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
		label.setOnMouseClicked(e -> removeContact(label));
		for(final Iterator<Node> i = getChildren().iterator(); i.hasNext();) {
			final Node n = i.next();
			if (n instanceof Label && ((Label) n).getText().equals(label.getText())) {
				// remove address if already in the list
				i.remove();
			}
		}
		LOGGER.debug("elements in flow pane: {}", getChildren().size());
		try {
			// add address in penultimate position
			getChildren().add(getChildren().size() - 1, label);
		} catch(final Exception e) {
			// seems sometime children list is empty (should contain the combo field at least)
			// add address in first position then...
			getChildren().add(0, label);
		}

		organise(label);
	}

	private void removeContact(final Label label) {
		getChildren().remove(label);
		organise();
	}

	private synchronized void organise() {
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
//			final double labelWidth = availableWidth < 150? 0: getWidth() - availableWidth;
//			combo.prefWidthProperty().bind(widthProperty().add(-1 * labelWidth));
		});
		ThreadPool.getInstance().submitHigh(organiseTask);
	}

	private String getLabel(final String contact) {
		return contact + " X";
	}

	private boolean isSelected(final String contact) {
		final String l = getLabel(contact);
		for (final Node n : getChildren()) {
			if (n instanceof Label && ((Label) n).getText().equals(l)) {
				return true;
			}
		}
		return false;
	}
}
