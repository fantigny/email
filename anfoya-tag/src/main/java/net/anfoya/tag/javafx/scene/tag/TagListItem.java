package net.anfoya.tag.javafx.scene.tag;

import net.anfoya.javafx.scene.control.IncExcListItem;
import net.anfoya.tag.service.Tag;

public class TagListItem<T extends Tag> extends IncExcListItem {
	private static final String COUNT_STRING = " (%d%s)";

	private final T tag;

	public TagListItem(final T tag) {
		this.tag = tag;

		setText();

		countProperty().addListener((ov, o, n) -> {
			setText();
			if (excludedProperty().get()) {
				disableProperty().set(false);
			} else {
				disableProperty().set(countProperty().get() == 0);
			}
		});

		excludedProperty().addListener((ov, o, n) -> {
			if (n) {
				includedProperty().set(false);
				disableProperty().set(false);
			}
		});
	}

	public T getTag() {
		return tag;
	}

	private void setText() {
		String text = getTag().getName();
		final long count = countProperty().get();
		if (count < 0) {
			text += String.format(COUNT_STRING, -count, "+");
		} else if (count > 0) {
			text += String.format(COUNT_STRING, count, "");
		}
		textProperty().set(text);
	}
}