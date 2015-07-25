package net.anfoya.javafx.scene.control;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import net.anfoya.javafx.scene.animation.DelayTimeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fantigny
 *
 * @param <T>
 */
public class ComboField<T> extends ComboBox<T> {
    private static final String DEFAULT_STYLE_CLASS = "combo-noarrow";
	private static final Logger LOGGER = LoggerFactory.getLogger(ComboField.class);

	private volatile T currentValue;
	private volatile T progSetValue;

	private EventHandler<ActionEvent> fieldActionHandler;
	private EventHandler<ActionEvent> listRequestHandler;
	private EventHandler<ActionEvent> emptyBackspaceHandler;

	private volatile boolean upHideReady;

	private DelayTimeline emptyTextDelay;
	private volatile boolean textIsEmpty;

	private DelayTimeline fieldActionDelay;

	public ComboField() {
		setEditable(true);
        getStyleClass().add(DEFAULT_STYLE_CLASS);

		currentValue = null;
		progSetValue = null;

		upHideReady = false;
		textIsEmpty = false;

		// fire <fieldActionHandler> on ENTER key released
		addEventFilter(KeyEvent.KEY_RELEASED, e -> {
			switch(e.getCode()) {
			case ENTER:
				LOGGER.debug("filter KEY_RELEASED ENTER");
				fireFieldAction(new ActionEvent(e.getSource(), e.getTarget()));
				break;
			default:
			}
		});

		// arm delayed <fieldActionHandler> on ComboBox::onAction event when showing
		// (to catch mouse click on combobox displayed list)
		setOnAction(event -> {
			if (showingProperty().get()) {
				LOGGER.debug("handle onAction and showing");
				fireDelayedFieldAction(event);
			}
		});

		// call handler when input is empty and the user hit backspace
		getEditor().addEventHandler(KeyEvent.KEY_RELEASED, e -> {
			if (textIsEmpty
					&& e.getCode() == KeyCode.BACK_SPACE
					&& emptyBackspaceHandler != null) {
				emptyBackspaceHandler.handle(null);
			}
		});

		// discard ENTER, ESCAPE, RIGHT, LEFT KEY_RELEASED
		// cancel delayed <fieldActionHandler> on UP or DOWN KEY_RELEASED
		// fire <listRequestHandler> on DOWN KEY_RELEASED (if list is not showing) or other characters
		getEditor().addEventFilter(KeyEvent.KEY_RELEASED, event -> {
			switch(event.getCode()) {
			case ENTER: case ESCAPE: case RIGHT: case LEFT: case SHIFT: case ALT: case WINDOWS: case COMMAND: case CONTROL: case TAB:
				LOGGER.debug("editor filter KEY_RELEASED ENTER/ESCAPE/RIGHT/LEFT/SHIFT/ALT/WINDOWS/COMMAND/CONTROL/TAB");
				break;
			case UP:
				LOGGER.debug("editor filter KEY_RELEASED UP");
				cancelDelayedFieldAction();
				if (showingProperty().get()) {
					if (getSelectionModel().isEmpty()) {
						LOGGER.debug("editor filter KEY_RELEASED UP and showing no selection");
						hide();
					} else {
						if (!upHideReady) {
							LOGGER.debug("editor filter KEY_RELEASED UP and showing");
						} else {
							LOGGER.debug("editor filter KEY_RELEASED UP again and showing");
							hide();
						}
						upHideReady = true;
					}
				}
				break;
			case DOWN:
				LOGGER.debug("editor filter KEY_RELEASED DOWN");
				cancelDelayedFieldAction();
				if (!showingProperty().get()) {
					LOGGER.debug("editor filter KEY_RELEASED DOWN and not showing");
					fireListRequest(new ActionEvent(event.getSource(), event.getTarget()));
				}
				break;
			default:
				LOGGER.debug("editor filter other characters");
				fireListRequest(new ActionEvent(event.getSource(), event.getTarget()));
				break;
			}
		});

		// keep track of the latest text input
		getEditor().textProperty().addListener((ov, o, n) -> {
			LOGGER.debug("handle text changed from \"{}\" to \"{}\"", o, n);
			currentValue = getConverter().fromString(n);
			upHideReady = false;
			if (getEditor().getText().isEmpty() && !o.isEmpty()) {
				if (emptyTextDelay != null) {
					emptyTextDelay.stop();
				}
				emptyTextDelay = new DelayTimeline(Duration.millis(200), e -> textIsEmpty = getEditor().getText().isEmpty());
				emptyTextDelay.play();
			} else {
				if (emptyTextDelay != null) {
					emptyTextDelay.stop();
				}
				textIsEmpty = getEditor().getText().isEmpty();
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
			this.currentValue = value;
			setValue(value);
		}
	}

	public void setOnListRequest(final EventHandler<ActionEvent> handler) {
		this.listRequestHandler = handler;
	}

	public void setOnFieldAction(final EventHandler<ActionEvent> handler) {
		this.fieldActionHandler = handler;
	}

	private void fireListRequest(final ActionEvent event) {
		if (listRequestHandler != null) {
			LOGGER.debug("fire list request");
			listRequestHandler.handle(event);
		}
	}

	private void fireDelayedFieldAction(final ActionEvent event) {
		cancelDelayedFieldAction();

		fieldActionDelay = new DelayTimeline(Duration.millis(500), e -> {
			LOGGER.debug("fire delayed field action");
			fireFieldAction(event);
		});
		fieldActionDelay.play();
	}

	private void cancelDelayedFieldAction() {
		if (fieldActionDelay != null) {
			fieldActionDelay.stop();
		}
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

	public void setOnBackspaceAction(final EventHandler<ActionEvent> handler) {
		emptyBackspaceHandler = handler;
	}
}