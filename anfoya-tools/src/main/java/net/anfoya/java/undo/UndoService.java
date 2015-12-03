package net.anfoya.java.undo;

import java.util.concurrent.Callable;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class UndoService {
	private final ReadOnlyObjectWrapper<Callable<Object>> callableProperty;
	private final ReadOnlyBooleanWrapper canUndoProperty;
	private String description;

	public UndoService() {
		callableProperty = new ReadOnlyObjectWrapper<Callable<Object>>();
		description = "";

		canUndoProperty = new ReadOnlyBooleanWrapper(false);
		canUndoProperty.bind(callableProperty.isNotNull());
	}

	public ReadOnlyBooleanProperty canUndoProperty() {
		return canUndoProperty.getReadOnlyProperty();
	}

	public void clear() {
		callableProperty.set(null);
		description = null;
	}

	public void set(Callable<Object> undoCall, String description) {
		clear();

		this.description = description;
		callableProperty.set(undoCall);
	}

	public synchronized void undo() throws UndoException {
		if (canUndoProperty.get()) {
			final Callable<Object> callable = callableProperty.get();
			clear();
			try {
				callable.call();
			} catch (final Exception e) {
				throw new UndoException(description, e);
			}
			clear();
		}
	}

	public String getDesciption() {
		return description;
	}
}
