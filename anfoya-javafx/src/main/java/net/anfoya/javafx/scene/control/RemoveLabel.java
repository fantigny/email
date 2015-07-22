package net.anfoya.javafx.scene.control;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RemoveLabel extends Label {
	private static final Color REGULAR_CROSS = Color.DARKGRAY;
	private static final Color HIGHLIGHT_CROSS = Color.BLACK;

	public RemoveLabel(final String text) {
		super(text);
		setGraphic(getResetButton());
		setContentDisplay(ContentDisplay.RIGHT);
		setAlignment(Pos.BOTTOM_LEFT);
	}

	private Group getResetButton() {
		final Rectangle r1 = getCard(-45, REGULAR_CROSS);
		final Rectangle r2 = getCard(45, REGULAR_CROSS);

		final Group group = new Group(r1, r2);
		group.setStyle("-fx-background-color: red;");
//		group.setLayoutX(group.getLayoutBounds().getMinX());
		setOnMouseEntered(t -> {
			r1.setFill(HIGHLIGHT_CROSS);
			r2.setFill(HIGHLIGHT_CROSS);
		});
		setOnMouseExited(t -> {
			r1.setFill(REGULAR_CROSS);
			r2.setFill(REGULAR_CROSS);
		});
		return group;
	}

	private Rectangle getCard(final int rotate, final Color fill) {
		final Rectangle card = new Rectangle(-4, -1, 8, 2);
		card.setStrokeWidth(1);
		card.setFill(fill);
		card.setRotate(rotate);
		return card;
	}
}