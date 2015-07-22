package net.anfoya.javafx.scene.control;

import javafx.scene.Group;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RemoveLabel extends Label {
	private static final Color REGULAR_CROSS = Color.DARKGRAY;
	private static final Color HIGHLIGHT_CROSS = Color.BLACK;

	public RemoveLabel(final String text) {
		final Rectangle r1 = getCard(-45, REGULAR_CROSS);
		final Rectangle r2 = getCard(45, REGULAR_CROSS);

		setOnMouseEntered(t -> {
			r1.setFill(HIGHLIGHT_CROSS);
			r2.setFill(HIGHLIGHT_CROSS);
		});
		setOnMouseExited(t -> {
			r1.setFill(REGULAR_CROSS);
			r2.setFill(REGULAR_CROSS);
		});

		setContentDisplay(ContentDisplay.RIGHT);
		setText(text);
		setGraphic(new Group(r1, r2));
	}

	private Rectangle getCard(final int rotate, final Color fill) {
		final Rectangle card = new Rectangle(-4, -1, 8, 2);
		card.setStrokeWidth(1);
		card.setFill(fill);
		card.setRotate(rotate);
		return card;
	}
}