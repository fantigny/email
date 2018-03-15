package net.anfoya.javafx.scene.control;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RemoveLabel extends Label {
	private static final Color REGULAR_CROSS = Color.DARKGRAY;
	private static final Color HIGHLIGHT_CROSS = Color.BLACK;

	public RemoveLabel(final String name) {
		this(name, null);
	}

	public RemoveLabel(final String text, final String toolTip) {
		if (toolTip != null) {
			setTooltip(new Tooltip(toolTip));
		}

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

		setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
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

	public void setOnRemove(final EventHandler<MouseEvent> handler) {
		setOnMouseClicked(handler);
	}
}