package net.anfoya.javafx.scene.control;

import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyEvent;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fantigny
 *
 * @param <T>
 */
public class ComboBoxField<T> extends ComboBox<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComboBoxField.class);

	private volatile T currentValue;
	private volatile T progSetValue;

	private final AtomicLong delayedActionId;

	private EventHandler<ActionEvent> fieldActionHandler;
	private EventHandler<ActionEvent> listRequestHandler;

	public ComboBoxField() {
		setEditable(true);

		currentValue = null;
		progSetValue = null;
		delayedActionId = new AtomicLong(0);

		// fire <fieldActionHandler> on ENTER key released
		addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				switch(event.getCode()) {
				case ENTER:
					LOGGER.debug("filter KEY_RELEASED ENTER");
					fireFieldAction(event);
					break;
				default:
				}
			}
		});

		// fire delayed <fieldActionHandler> on ComboBox::onAction event
		setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				LOGGER.debug("handle onAction");
				fireDelayedFieldAction(event);
			}
		});

		// discard ENTER, ESCAPE, RIGHT, LEFT KEY_RELEASED
		// cancel delayed <fieldActionHandler> on UP or DOWN KEY_RELEASED
		// fire <listRequestHandler> on DOWN KEY_RELEASED (if list is not showing) or other characters
		getEditor().addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				switch(event.getCode()) {
				case ENTER: case ESCAPE: case RIGHT: case LEFT:
					LOGGER.debug("editor discard KEY_PRESSED ENTER/ESCAPE/RIGHT/LEFT");
					break;
				case UP:
					LOGGER.debug("editor handle KEY_PRESSED UP");
					cancelDelayedFieldAction();
					break;
				case DOWN:
					LOGGER.debug("editor handle KEY_PRESSED DOWN");
					cancelDelayedFieldAction();
					if (!isShowing()) {
						LOGGER.debug("editor handle KEY_PRESSED DOWN and not showing");
						fireListRequest(event);
					}
					break;
				default:
					LOGGER.debug("editor handle other characters");
					fireListRequest(event);
					break;
				}
			}
		});

		// keep track of the latest text input
		getEditor().textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> ov, final String oldVal, final String newVal) {
				LOGGER.debug("handle text changed ", newVal);
				currentValue = getConverter().fromString(newVal);
			}
		});
	}

	public T getFieldValue() {
		final T value = getValue();
		if (currentValue != null && currentValue.equals(value)) {
			currentValue = value;
		}
		LOGGER.debug("get field value ({})", currentValue);
		return currentValue;
	}

	public void setFieldValue(final T value) {
		LOGGER.debug("set field value ({})", value);
		if (!value.equals(this.progSetValue)) {
			this.progSetValue = value;
			setValue(value);
		}
	}

	public void setOnListRequest(final EventHandler<ActionEvent> handler) {
		this.listRequestHandler = handler;
	}

	public void setOnFieldAction(final EventHandler<ActionEvent> handler) {
		this.fieldActionHandler = handler;
	}

	private void fireListRequest(final KeyEvent event) {
		if (listRequestHandler != null) {
			LOGGER.debug("call list request handler");
			listRequestHandler.handle(new ActionEvent(event.getSource(), event.getTarget()));
		}
	}

	private void fireDelayedFieldAction(final Event event) {
		final long submitSearchId = ComboBoxField.this.delayedActionId.incrementAndGet();
		LOGGER.debug("delayed field action ({})", submitSearchId);
		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try { Thread.sleep(200); }
				catch(final Exception e) { return; }
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						if (submitSearchId == ComboBoxField.this.delayedActionId.get()) {
							// only submit search if this is not a up/down keyboard action, i.e. it is a mouse click
							LOGGER.debug("fire field action ({})", submitSearchId);
							fireFieldAction(event);
						}
					}
				});
			}
		});
	}

	private void cancelDelayedFieldAction() {
		delayedActionId.incrementAndGet();
	}

	private void fireFieldAction(final Event event) {
		fireFieldAction(new ActionEvent(event.getSource(), event.getTarget()));
	}

	private void fireFieldAction(final ActionEvent event) {
		LOGGER.debug("fire field action");
		currentValue = getFieldValue();

		if (currentValue != null && currentValue.equals(progSetValue)) {
			LOGGER.debug("cancelled (set by prog)");
			return;
		}
		progSetValue = currentValue;

		if (fieldActionHandler != null) {
			LOGGER.debug("call field action handler");
			fieldActionHandler.handle(event);
		}
	}
}