package net.anfoya.javafx.scene.dnd;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class DndHelper {

	public static Image textToImage(String text) {
		final Label label = new Label(text);
		label.setStyle("-fx-padding: 2 5 2 5;"
				+ "-fx-background-color: transparent;"
				+ "-fx-border-width: 1;"
				+ "-fx-border-color: darkgray;"
				+ "-fx-border-radius: 5;"
				+ "-fx-text-fill: black;"
				+ "-fx-font-size: 13px;");

		final Scene scene = new Scene(label);

		WritableImage img = new WritableImage(125, 125);
		scene.snapshot(img);
		img = new WritableImage((int)label.getWidth(), (int)label.getHeight());
		scene.snapshot(img);

		return img;
	}
}
