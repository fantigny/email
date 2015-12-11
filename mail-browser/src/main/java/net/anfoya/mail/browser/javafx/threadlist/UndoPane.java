package net.anfoya.mail.browser.javafx.threadlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import net.anfoya.java.undo.UndoService;

public class UndoPane extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(UndoPane.class);

	public UndoPane(UndoService service, Node focusNode) {
		getStyleClass().add("droparea-grid");
		setVisible(false);
		setOpacity(0);
		setMinHeight(40);
		setMaxHeight(40);
		managedProperty().bind(visibleProperty());

		final Button button = new Button("undo");
		button.getStyleClass().add("undo");
		button.prefWidthProperty().bind(widthProperty());
		button.prefHeightProperty().bind(heightProperty());
		button.setOnAction(e -> {
			try {
				service.undo();
				focusNode.requestFocus();
			} catch (final Exception ex) {
				LOGGER.error("undo", ex);
			}
		});
		add(button, 0, 0);

		final Timeline timeline = new Timeline(
				new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0))
				, new KeyFrame(Duration.seconds(.5), new KeyValue(opacityProperty(), 1))
				, new KeyFrame(Duration.seconds(10), new KeyValue(opacityProperty(), 1))
				, new KeyFrame(Duration.seconds(12), new KeyValue(opacityProperty(), 0))
				);
		timeline.setOnFinished(e -> {
			setVisible(false);
		});

		service.canUndoProperty().addListener((ov, o, n) -> Platform.runLater(() -> {
			setVisible(n);
			if (n) {
				button.setText("undo " + service.getDesciption());
				timeline.playFromStart();
			}
		}));

		setOnMouseEntered(e -> {
			timeline.stop();
			setOpacity(1);
		});
		setOnMouseExited(e -> timeline.playFrom(Duration.seconds(.5)));
	}
}
