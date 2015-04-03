package net.anfoya.movie.browser.javafx.taglist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.anfoya.movie.browser.model.Tag;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;

public class SelectedTagsPane extends FlowPane {

	protected static final String CROSS = " X";
	private Callback<String, Void> delTagCallBack;

	public SelectedTagsPane() {
	    setPadding(new Insets(5, 0, 5, 0));
		setVgap(3);
		setHgap(5);
		setPrefWidth(0);
	}

	public void refresh(final Set<Tag> tags) {
		final List<Button> buttons = new ArrayList<Button>();
		for(final Tag tag: tags) {
			final Button button = new Button(tag.getName() + CROSS);
			button.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					delTagCallBack.call(tag.getName());
				}
			});
			buttons.add(button);
		}
		getChildren().setAll(buttons);
	}

	public void setDelTagCallBack(final Callback<String, Void> callback) {
		this.delTagCallBack = callback;
	}
}
