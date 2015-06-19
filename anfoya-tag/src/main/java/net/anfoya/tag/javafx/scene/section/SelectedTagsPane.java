package net.anfoya.tag.javafx.scene.section;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import net.anfoya.tag.model.SimpleTag;

public class SelectedTagsPane<T extends SimpleTag> extends FlowPane {

	protected static final String CROSS = " X";
	private Callback<T, Void> clearTagCallBack;

	public SelectedTagsPane() {
		setVgap(3);
		setHgap(3);
		setPadding(new Insets(3));
	}

	public void refresh(final Set<T> tags) {
		final List<Button> buttons = new ArrayList<Button>();
		for(final T tag: tags) {
			final Button button = new Button(tag.getName() + CROSS);
			button.getStyleClass().add("tag-button");
			button.setOnAction(event -> {
				if (clearTagCallBack != null) {
					SelectedTagsPane.this.getChildren().remove(button);
					clearTagCallBack.call(tag);
				}
			});
			buttons.add(button);
		}
		getChildren().setAll(buttons);
	}

	public void setClearTagCallBack(final Callback<T, Void> callback) {
		this.clearTagCallBack = callback;
	}

	public void clear() {
		getChildren().clear();
	}
}
