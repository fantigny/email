package net.anfoya.mail.browser.javafx.threadlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import net.anfoya.mail.service.MailService;

public class UndoPane extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(UndoPane.class);

	public UndoPane(MailService<?, ?, ?, ?, ?> mailService) {
		getStyleClass().add("droparea-grid");
		setMaxHeight(65);

		setVisible(false);
		managedProperty().bind(visibleProperty());

		final Button button = new Button("undo");
		button.getStyleClass().add("dropbutton-box");
		button.prefWidthProperty().bind(widthProperty());
		button.prefHeightProperty().bind(heightProperty());
		button.setOnAction(e -> {
			try {
				mailService.undo();
			} catch (final Exception ex) {
				LOGGER.error("undoing", ex);
			}
		});
		add(button, 0, 0);

		mailService.canUndo().addListener((ov, o, n) -> Platform.runLater(() -> setVisible(n)));
		mailService.undoDescritpion().addListener((ov, o, n) -> Platform.runLater(() -> button.setText("undo " + mailService.undoDescritpion().get())));
	}
}
