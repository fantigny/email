package net.anfoya.javafx.util;

import javafx.scene.control.Label;

public class LabelHelper {
	public static final double computeWidth(final Label label) {
		
		return label.getBoundsInLocal().getWidth()
				+ label.getPadding().getLeft()
				+ label.getPadding().getRight();
	}
}
