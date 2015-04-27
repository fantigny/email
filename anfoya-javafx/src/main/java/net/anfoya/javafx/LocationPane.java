package net.anfoya.javafx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class LocationPane extends BorderPane {
	private final Button backwardButton;
	private final Button forwwardButton;
	private final Button homeButton;
	private final TextField text;

	private final HBox leftBox;
	private final Button reloadButton;
	private final Button stopButton;
	private final BooleanProperty loadingProperty;

	public LocationPane() {
		setPadding(new Insets(5));

		backwardButton = new Button();
		backwardButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("Backward.png"))));
		backwardButton.setDisable(true);
		forwwardButton = new Button();
		forwwardButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("Forward.png"))));
		forwwardButton.setDisable(true);
		homeButton = new Button();
		homeButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("Home.png"))));
		reloadButton = new Button();
		reloadButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("Refresh.png"))));
		stopButton = new Button();
		stopButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("Cancel.png"))));

		leftBox = new HBox(3, backwardButton, forwwardButton, homeButton);
		setLeft(leftBox);

		loadingProperty = new SimpleBooleanProperty();
		loadingProperty.addListener((ov, oldVal, newVal) -> {
			leftBox.getChildren().remove(newVal? reloadButton: stopButton);
			leftBox.getChildren().add(newVal? stopButton: reloadButton);
		});

		text = new TextField();
		text.setDisable(true);

		setMargin(text, new Insets(0, 3, 0, 3));
		setCenter(text);
	}

	public void setOnAction(final EventHandler<ActionEvent> handler) {
		text.setOnAction(handler);
		text.setDisable(handler == null);
		text.setEditable(handler != null);
	}

	public void setOnHomeAction(final EventHandler<ActionEvent> handler) {
		homeButton.setOnAction(handler);
	}

	public void setOnReloadAction(final EventHandler<ActionEvent> handler) {
		reloadButton.setOnAction(handler);
	}

	public void setOnBackAction(final EventHandler<ActionEvent> handler) {
		backwardButton.setOnAction(handler);
	}

	public void setOnForwardAction(final EventHandler<ActionEvent> handler) {
		forwwardButton.setOnAction(handler);
	}

	public void setOnStopAction(final EventHandler<ActionEvent> handler) {
		stopButton.setOnAction(handler);
	}

	public StringProperty locationProperty() {
		return text.textProperty();
	}

	public BooleanProperty runningProperty() {
		return loadingProperty;
	}

	public BooleanProperty backwardDisableProperty() {
		return backwardButton.disableProperty();
	}

	public BooleanProperty forwardDisableProperty() {
		return forwwardButton.disableProperty();
	}
}