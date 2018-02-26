package net.anfoya.javafx.scene.control;

import javafx.scene.Node;
import javafx.scene.control.ToolBar;
import javafx.scene.web.HTMLEditor;

public class HtmlEditorToolBarHelper {
	public enum Line { TOP, BOTTOM };

	private final ToolBar topToolBar;
	private final ToolBar botToolBar;

	public HtmlEditorToolBarHelper(HTMLEditor editor) {
		topToolBar = (ToolBar) editor.lookupAll(".top-toolbar").iterator().next();
		botToolBar = (ToolBar) editor.lookupAll(".bottom-toolbar").iterator().next();
	}

	public void hideToolBar(Line line) {
		getToolBar(line).setVisible(false);
		getToolBar(line).setManaged(false);
	}

	public void removeItem(Line line, int pos) {
		getToolBar(line).getItems().remove(pos);
	}

	public void moveToolbarItem(Line srcLine, int srcPos, Line tgtLine, int tgtPos) {
		final ToolBar srcToolbar = getToolBar(srcLine);
		final Node node = srcToolbar.getItems().get(srcPos);
		if (srcLine != tgtLine) {
			srcToolbar.getItems().remove(node);
		}
		getToolBar(tgtLine).getItems().add(tgtPos, node);
	}

	private ToolBar getToolBar(Line line) {
		switch(line) {
		case TOP:
			return topToolBar;
		case BOTTOM:
			return botToolBar;
		default:
			return null;
		}
	}
}
