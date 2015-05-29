package net.anfoya.mail.browser.javafx.settings;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;

public class SwitchButton extends Label {
	private final SimpleBooleanProperty switchedOn;

	public SwitchButton() {
		switchedOn = new SimpleBooleanProperty(true);
		switchedOn.addListener((ov, o, n) -> {
			if (n) {
				setText(" on");
				setStyle("-fx-background-color: green;-fx-text-fill:white;");
				setContentDisplay(ContentDisplay.RIGHT);
			} else {
				setText("off ");
				setStyle("-fx-background-color: grey;-fx-text-fill:black;");
				setContentDisplay(ContentDisplay.LEFT);
			}
		});
		switchedOn.set(false);

		final Button switchBtn = new Button();
		switchBtn.setPrefWidth(40);
		switchBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent t) {
				switchedOn.set(!switchedOn.get());
			}
		});

		setGraphic(switchBtn);
	}

	public SimpleBooleanProperty switchOnProperty() {
		return switchedOn;
	}

	public boolean isSwitchOn() {
		return switchOnProperty().get();
	}

	public void setSwitchOn(final boolean on) {
		switchOnProperty().set(on);
	}
}