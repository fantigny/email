package net.anfoya.tag.javafx.scene.control;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import net.anfoya.tag.model.Tag;

public class SelectedTagsPane extends FlowPane {

	protected static final String CROSS = " X";
	private Callback<String, Void> delTagCallBack;

	public SelectedTagsPane() {
		setVgap(3);
		setHgap(3);
		setPrefWidth(0);
	}

	public void refresh(final List<Tag> list) {
		final List<Button> buttons = new ArrayList<Button>();
		for(final Tag tag: list) {
			final Button button = new Button(tag.getName() + CROSS);
			button.setOnAction(event -> delTagCallBack.call(tag.getName()));
			buttons.add(button);
		}
		getChildren().setAll(buttons);
	}

	public void setDelTagCallBack(final Callback<String, Void> callback) {
		delTagCallBack = callback;
	}
}
