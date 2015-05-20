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
	private final TextField delegate;
	private final Group resetButton;

	public ResetTextField() {
		setAlignment(Pos.CENTER_RIGHT);

		delegate = new TextField();
		delegate.prefWidthProperty().bind(widthProperty());
		delegate.textProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal.isEmpty()) {
				resetButton.setVisible(false);
			} else {
				resetButton.setVisible(true);
			}
		});
		getChildren().add(delegate);

		resetButton = getResetButton();
		resetButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent t) {
				delegate.clear();
			}
		});
		setMargin(resetButton, new Insets(0, 5, 0, 0));
		getChildren().add(resetButton);
	}

	private Group getResetButton() {
		final Circle circle = new Circle(7.0, Color.web("#4c4c4c"));
		final Rectangle r1 = getCard(-45);
		final Rectangle r2 = getCard(45);

		final Group group = new Group(circle, r1, r2);
		group.setVisible(false);
		group.setLayoutX(group.getLayoutBounds().getMinX() - 3);
		group.setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent t) {
				circle.setFill(Color.web("#097dda"));
				r1.setFill(Color.WHITE);
				r2.setFill(Color.WHITE);
			}
		});
		group.setOnMouseExited(new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent t) {
				circle.setFill(Color.web("#4c4c4c"));
				r1.setFill(Color.web("#868686"));
				r2.setFill(Color.web("#868686"));
			}
		});
		return group;
	}

	private Rectangle getCard(final int rotate) {
		final Rectangle card = new Rectangle(-4, -1, 8, 2);
		card.setStrokeWidth(1);
		card.setFill(Color.web("#868686"));
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