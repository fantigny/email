package net.anfoya.mail.gmail.service;

import java.util.concurrent.Callable;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public class UndoService {
	private final ReadOnlyObjectWrapper<Callable<Object>> callableProperty;
	private final ReadOnlyBooleanWrapper canUndoProperty;
	private final ReadOnlyStringWrapper descriptionProperty;

	public UndoService() {
		callableProperty = new ReadOnlyObjectWrapper<Callable<Object>>();
		descriptionProperty = new ReadOnlyStringWrapper();

		canUndoProperty = new ReadOnlyBooleanWrapper(false);
		canUndoProperty.bind(callableProperty.isNotNull());
	}

	public ReadOnlyBooleanProperty canUndoProperty() {
		return canUndoProperty.getReadOnlyProperty();
	}

	public void reset() {
		set(null, null);
	}

	public void set(Callable<Object> undoCall, String description) {
		callableProperty.set(undoCall);
		descriptionProperty.set(description);
	}

	public synchronized void undo() throws UndoException {
		if (canUndoProperty.get()) {
			final Callable<Object> callable = callableProperty.get();
			reset();
			try {
				callable.call();
			} catch (final Exception e) {
				throw new UndoException("undoing " + descriptionProperty.get(), e);
			}
			reset();
		}
	}

	public ReadOnlyStringProperty descritpionProperty() {
		return descriptionProperty;
	}
}
