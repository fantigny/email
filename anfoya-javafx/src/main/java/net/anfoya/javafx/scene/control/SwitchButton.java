package net.anfoya.javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;

public class SwitchButton extends Label {
	private final BooleanProperty enabled;
	private final BooleanProperty switchedOn;

	public SwitchButton() {
		ChangeListener<Boolean> switchListener = (ov, o, n) -> {
			if (n) {
				setText(" on");
				setStyle("-fx-background-color: green; -fx-text-fill: white;");
				setContentDisplay(ContentDisplay.RIGHT);
			} else {
				setText("off ");
				setStyle("-fx-background-color: grey; -fx-text-fill: black;");
				setContentDisplay(ContentDisplay.LEFT);
			}
		};
		switchedOn = new SimpleBooleanProperty(true);
		
		enabled = new SimpleBooleanProperty(true);
		enabled.addListener((ov, o, n) -> {
			if (n) {
				switchListener.changed(switchedOn, !switchedOn.get(), switchedOn.get());
				switchedOn.addListener(switchListener);
			} else {
				setStyle("-fx-background-color: darkgrey; -fx-text-fill: grey;");
				switchedOn.removeListener(switchListener);
			}
		});		

		final Button switchBtn = new Button();
		switchBtn.setPrefWidth(40);
		switchBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent t) {
				switchedOn.set(!switchedOn.get());
			}
		});

		// initialise graphic state
		switchedOn.addListener(switchListener);
		switchedOn.set(false);
		
		setGraphic(switchBtn);
	}
	
	public BooleanProperty enabledProperty() {
		return enabled;
	}

	public BooleanProperty switchOnProperty() {
		return switchedOn;
	}

	public boolean isSwitchOn() {
		return switchOnProperty().get();
	}

	public void setSwitchOn(final boolean on) {
		switchOnProperty().set(on);
	}
}