package net.anfoya.tag.javafx.scene.section;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import net.anfoya.java.util.VoidCallback;
import net.anfoya.javafx.scene.control.RemoveLabel;
import net.anfoya.tag.service.Tag;

public class SelectedTagsPane<T extends Tag> extends FlowPane {

	private VoidCallback<T> removeTagCallBack;

	public SelectedTagsPane() {
		getStyleClass().add("label-list-pane");
		setVgap(3);
		setHgap(3);
		setPadding(new Insets(3));
	}

	public void refresh(final Set<T> tags) {
		final Set<Label> labels = new LinkedHashSet<Label>();
		final Set<T> sortedTags = new TreeSet<T>(tags);
		for(final T tag: sortedTags) {
			if (tag.isSystem()) {
				labels.add(createLabel(tag));
			}
		}

		for(final T tag: sortedTags) {
			if (!tag.isSystem()) {
				labels.add(createLabel(tag));
			}
		}
		getChildren().setAll(labels);
	}

	private Label createLabel(final T tag) {
		final RemoveLabel label = new RemoveLabel(tag.getName(), "remove label");
		label.setOnRemove(e -> {
			if (removeTagCallBack != null) {
				getChildren().remove(label);
				removeTagCallBack.call(tag);
			}
		});
		return label;
	}

	public void setRemoveTagCallBack(final VoidCallback<T> callback) {
		this.removeTagCallBack = callback;
	}

	public void clear() {
		getChildren().clear();
	}
}
