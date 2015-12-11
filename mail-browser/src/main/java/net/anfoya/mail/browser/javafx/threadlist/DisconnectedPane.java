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
import net.anfoya.mail.service.MailService;

public class DisconnectedPane extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(DisconnectedPane.class);

	public DisconnectedPane(MailService<?, ?, ?, ?, ?> mailService, Node focusNode) {
		getStyleClass().add("droparea-grid");
		setVisible(false);
		setOpacity(0);
		setMinHeight(40);
		setMaxHeight(40);
		managedProperty().bind(visibleProperty());

		final Button button = new Button("disconnected");
		button.getStyleClass().add("disconnected");
		button.prefWidthProperty().bind(widthProperty());
		button.prefHeightProperty().bind(heightProperty());
		button.setOnAction(e -> {
			try {
				mailService.reconnect();
				focusNode.requestFocus();
			} catch (final Exception ex) {
				LOGGER.error("reconnect", ex);
			}
		});
		add(button, 0, 0);

		final Timeline timeline = new Timeline(
				new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0))
				, new KeyFrame(Duration.seconds(.5), new KeyValue(opacityProperty(), 1))
				);
		timeline.setOnFinished(e -> setVisible(mailService.disconnectedProperty().get()));

		mailService.disconnectedProperty().addListener((ov, o, n) -> Platform.runLater(() -> {
			if (n) {
				setVisible(true);
				timeline.setRate(1);
				timeline.playFromStart();
			} else {
				timeline.setRate(-1);
				timeline.playFrom(Duration.seconds(.5));
			}
		}));

		setOnMouseEntered(e -> {
			timeline.stop();
			setOpacity(1);
		});
		setOnMouseExited(e -> timeline.playFrom(Duration.seconds(.5)));
	}
}
