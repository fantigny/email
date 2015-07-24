package net.anfoya.tag.javafx.scene.section;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import net.anfoya.tag.service.Tag;

public class SelectedTagsPane<T extends Tag> extends FlowPane {

	protected static final String CROSS = " X";
	private Callback<T, Void> clearTagCallBack;

	public SelectedTagsPane() {
		setVgap(3);
		setHgap(3);
		setPadding(new Insets(3));
	}

	public void refresh(final Set<T> tags) {
		final List<Button> buttons = new ArrayList<Button>();
		final Set<T> sortedTags = new TreeSet<T>(tags);
		for(final T tag: sortedTags) {
			if (tag.isSystem()) {
				buttons.add(createButton(tag));
			}
		}
		for(final T tag: sortedTags) {
			if (!tag.isSystem()) {
				buttons.add(createButton(tag));
			}
		}
		getChildren().setAll(buttons);
	}

	private Button createButton(final T tag) {
		final Button button = new Button(tag.getName() + CROSS);
		button.getStyleClass().add("tag-label");
		button.setOnAction(event -> {
			if (clearTagCallBack != null) {
				SelectedTagsPane.this.getChildren().remove(button);
				clearTagCallBack.call(tag);
			}
		});
		return button;
	}

	public void setClearTagCallBack(final Callback<T, Void> callback) {
		this.clearTagCallBack = callback;
	}

	public void clear() {
		getChildren().clear();
	}
}
