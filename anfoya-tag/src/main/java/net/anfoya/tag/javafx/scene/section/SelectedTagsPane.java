package net.anfoya.tag.javafx.scene.section;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import net.anfoya.tag.model.SimpleTag;

public class SelectedTagsPane<T extends SimpleTag> extends FlowPane {

	protected static final String CROSS = " X";
	private Callback<T, Void> delTagCallBack;

	public SelectedTagsPane() {
		setVgap(3);
		setHgap(3);
		setPrefWidth(0);
	}

	public void refresh(final Set<T> tags) {
		final List<Button> buttons = new ArrayList<Button>();
		for(final T tag: tags) {
			final Button button = new Button(tag.getName() + CROSS);
			button.setOnAction(event -> {
				if (delTagCallBack != null) {
					SelectedTagsPane.this.getChildren().remove(button);
					delTagCallBack.call(tag);
				}
			});
			buttons.add(button);
		}
		getChildren().setAll(buttons);
	}

	public void setDelTagCallBack(final Callback<T, Void> callback) {
		this.delTagCallBack = callback;
	}

	public void clear() {
		getChildren().clear();
	}
}
