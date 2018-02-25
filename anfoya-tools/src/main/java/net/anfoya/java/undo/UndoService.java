package net.anfoya.java.undo;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import net.anfoya.java.util.VoidCallable;

public class UndoService {
	private final ReadOnlyObjectWrapper<VoidCallable> callbackProperty;
	private final ReadOnlyBooleanWrapper canUndoProperty;
	private String description;

	public UndoService() {
		callbackProperty = new ReadOnlyObjectWrapper<>();
		description = "";

		canUndoProperty = new ReadOnlyBooleanWrapper(false);
		canUndoProperty.bind(callbackProperty.isNotNull());
	}

	public ReadOnlyBooleanProperty canUndoProperty() {
		return canUndoProperty.getReadOnlyProperty();
	}

	public void clear() {
		synchronized (callbackProperty) {
			callbackProperty.set(null);
			description = null;
		}
	}

	public void setUndo(VoidCallable callback, String description) {
		clear();

		synchronized (callbackProperty) {
			callbackProperty.set(callback);
			this.description = description;
		}
	}

	public synchronized void undo() throws UndoException {
		if (canUndoProperty.get()) {
			final VoidCallable callback;
			final String description;
			synchronized (callbackProperty) {
				callback = callbackProperty.get();
				description = this.description;
			}
			clear();
			if (callback != null) {
				try {
					callback.call();
				} catch (final Exception e) {
					throw new UndoException(description, e);
				}
			}
			clear();
		}
	}

	public String getDesciption() {
		return description;
	}
}
