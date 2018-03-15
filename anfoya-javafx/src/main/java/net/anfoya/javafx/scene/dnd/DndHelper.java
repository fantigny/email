package net.anfoya.javafx.scene.dnd;

import java.util.List;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class DndHelper {

	private final List<String> stylesheets;

	public DndHelper(List<String> stylesheets) {
		this.stylesheets = stylesheets;
	}

	public Image textToImage(String text) {
		final Label label = new Label(text);
		label.getStyleClass().add("label-dnd");

		return toImage(label);
	}

	public Image toImage(Node node) {
		final Scene scene = new Scene(new Group(node), Color.TRANSPARENT);
		scene.getStylesheets().setAll(stylesheets);

		WritableImage img = new WritableImage(200, 75);
		scene.snapshot(img);
		img = new WritableImage((int)scene.getWidth()+1, (int)scene.getHeight());
		scene.snapshot(img);

		return img;
	}
}
