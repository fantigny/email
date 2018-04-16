package net.anfoya.mail.browser.javafx.threadlist;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

public class DisconnectedPane extends GridPane {
	private final BooleanProperty disconnected;
	private final Timeline timeline;

	private Runnable reconnectCallback;

	public DisconnectedPane() {
		disconnected = new SimpleBooleanProperty();
		timeline = new Timeline(
				new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0))
				, new KeyFrame(Duration.seconds(.5), new KeyValue(opacityProperty(), 1))
				);

		getStyleClass().add("droparea-grid");
		setVisible(false);
		setOpacity(0);
		setMinHeight(40);
		setMaxHeight(40);
		managedProperty().bind(visibleProperty());

		final Button button = new Button("disconnected");
		button.setFocusTraversable(false);
		button.getStyleClass().add("disconnected");
		button.prefWidthProperty().bind(widthProperty());
		button.prefHeightProperty().bind(heightProperty());
		button.setOnAction(e -> reconnectCallback.run());
		add(button, 0, 0);

		timeline.setOnFinished(e -> setVisible(disconnected.get()));

		disconnected.addListener((ov, o, n) -> {
			if (n) {
				Platform.runLater(() -> setVisible(true));
				timeline.setRate(1);
				timeline.playFromStart();
			} else {
				timeline.setRate(-2);
				timeline.playFrom(Duration.seconds(.5));
			}
		});

	}

	public void setOnReconnect(Runnable reconnectCallback) {
		this.reconnectCallback = reconnectCallback;
	}

	public BooleanProperty disconnected() {
		return disconnected;
	}
}
