package net.anfoya.javafx.scene.control;

import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class ResetTextField extends StackPane {
	private static final Color REGULAR_CIRCLE = Color.web("#4c4c4c");
	private static final Color REGULAR_CROSS = Color.web("#868686");
	private static final Color HIGHLIGHT_CIRCLE = Color.web("#097dda");
	private static final Color HIGHLIGHT_CROSS = Color.WHITE;

	private final TextField delegate = new TextField();
	private final Group resetButton = getResetButton();

	public ResetTextField() {
		getChildren().addAll(delegate, resetButton);
		setAlignment(Pos.CENTER_RIGHT);
		setMargin(resetButton, new Insets(0, 5, 0, 0));

		delegate.prefWidthProperty().bind(widthProperty());
		delegate.textProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal.isEmpty()) {
				resetButton.setVisible(false);
			} else {
				resetButton.setVisible(true);
			}
		});

		resetButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent t) {
				delegate.clear();
			}
		});
	}

	private Group getResetButton() {
		final Circle circle = new Circle(7.0, REGULAR_CIRCLE);
		final Rectangle r1 = getCard(-45, REGULAR_CROSS);
		final Rectangle r2 = getCard(45, REGULAR_CROSS);

		final Group group = new Group(circle, r1, r2);
		group.setVisible(false);
		group.setLayoutX(group.getLayoutBounds().getMinX() - 3);
		group.setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent t) {
				circle.setFill(HIGHLIGHT_CIRCLE);
				r1.setFill(HIGHLIGHT_CROSS);
				r2.setFill(HIGHLIGHT_CROSS);
			}
		});
		group.setOnMouseExited(new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent t) {
				circle.setFill(REGULAR_CIRCLE);
				r1.setFill(REGULAR_CROSS);
				r2.setFill(REGULAR_CROSS);
			}
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

	public void setPromptText(final String text) {
		delegate.setPromptText(text);
	}

	public StringProperty textProperty() {
		return delegate.textProperty();
	}

	public String getText() {
		return delegate.getText();
	}
}