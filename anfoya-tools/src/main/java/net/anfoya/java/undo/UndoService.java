package net.anfoya.java.undo;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import net.anfoya.java.util.VoidCallable;

public class UndoService {
	private final ReadOnlyObjectWrapper<VoidCallable> callableProperty;
	private final ReadOnlyBooleanWrapper canUndoProperty;
	private String description;

	public UndoService() {
		callableProperty = new ReadOnlyObjectWrapper<VoidCallable>();
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

	public void set(VoidCallable undoCall, String description) {
		clear();

		this.description = description;
		callableProperty.set(undoCall);
	}

	public synchronized void undo() throws UndoException {
		if (canUndoProperty.get()) {
			final VoidCallable callable = callableProperty.get();
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
