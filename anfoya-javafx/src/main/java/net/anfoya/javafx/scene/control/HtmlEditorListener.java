package net.anfoya.javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.HTMLEditor;

public class HtmlEditorListener {
	private final BooleanProperty editedProperty;

	private String htmlRef;

	public HtmlEditorListener(final HTMLEditor editor) {
		editedProperty = new SimpleBooleanProperty();
		editedProperty.addListener((ov, o, n) -> htmlRef = n? null: editor.getHtmlText());
		editedProperty.set(false);

		editor.setOnMouseClicked(e -> checkEdition(editor.getHtmlText()));
		editor.addEventFilter(KeyEvent.KEY_TYPED, e -> checkEdition(editor.getHtmlText()));
	}

	public BooleanProperty editedProperty() {
		return editedProperty;
	}

	private void checkEdition(final String html) {
		if (editedProperty.get()) {
			return;
		}
		editedProperty.set(htmlRef != null
				&& html.length() != htmlRef.length()
				|| !html.equals(htmlRef));
	}
}
