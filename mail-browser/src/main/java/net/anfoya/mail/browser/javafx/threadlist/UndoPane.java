package net.anfoya.mail.browser.javafx.threadlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import net.anfoya.mail.gmail.service.UndoService;

public class UndoPane extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(UndoPane.class);
	private Timeline timeline;

	public UndoPane(UndoService service) {
		getStyleClass().add("droparea-grid");
		setMaxHeight(40);
		setVisible(false);
		setOpacity(0);
		managedProperty().bind(visibleProperty());

		final Button button = new Button("undo");
		button.getStyleClass().add("undo");
		button.prefWidthProperty().bind(widthProperty());
		button.prefHeightProperty().bind(heightProperty());
		button.setOnAction(e -> {
			try {
				service.undo();
			} catch (final Exception ex) {
				LOGGER.error("undoing", ex);
			}
		});
		add(button, 0, 0);

		// show myself when mail service can undo
		service.canUndoProperty().addListener((ov, o, n) -> Platform.runLater(() -> setVisible(n)));

		// keep undo description up to date and vanish after delay
		service.descritpionProperty().addListener((ov, o, n) -> Platform.runLater(() -> {
			button.setText("undo " + n);

			if (timeline != null) {
				timeline.stop();
			}
			if (isVisible()) {
				timeline = new Timeline(
						new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0))
						, new KeyFrame(Duration.seconds(.5), new KeyValue(opacityProperty(), 1))
						, new KeyFrame(Duration.seconds(10), new KeyValue(opacityProperty(), 1))
						, new KeyFrame(Duration.seconds(12), new KeyValue(opacityProperty(), 0))
						);
				timeline.play();
			}
		}));
	}
}
