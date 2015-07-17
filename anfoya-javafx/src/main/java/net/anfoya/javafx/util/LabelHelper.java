package net.anfoya.javafx.util;

import javafx.scene.control.Label;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

public class LabelHelper {
	private static final FontLoader FONT_LOADER = Toolkit.getToolkit().getFontLoader();

	public static final double computeWidth(final Label label) {
		return FONT_LOADER.computeStringWidth(label.getText(), label.getFont())
				+ label.getPadding().getLeft()
				+ label.getPadding().getRight();
	}
}
