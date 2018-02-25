package net.anfoya.javafx.scene.control;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.Node;
import javafx.scene.control.ToolBar;
import javafx.scene.web.HTMLEditor;
import net.anfoya.java.util.concurrent.ThreadPool;

public class HtmlEditorToolBarHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlEditorToolBarHelper.class);
	public enum Line { TOP, BOTTOM };

	private final List<Runnable> actions;
	private final CountDownLatch countDownLatch;

	private final ToolBar topToolBar;
	private final ToolBar botToolBar;

	public HtmlEditorToolBarHelper(HTMLEditor editor) {
		actions = new LinkedList<>();
		countDownLatch = new CountDownLatch(2);

		topToolBar = (ToolBar) editor.lookupAll(".top-toolbar").iterator().next();
		topToolBar.getItems().addListener((Change<? extends Node> c) -> countDownLatch.countDown());

		botToolBar = (ToolBar) editor.lookupAll(".bottom-toolbar").iterator().next();
		botToolBar.getItems().addListener((Change<? extends Node> c) -> countDownLatch.countDown());
	}

	public void hideToolBar(Line line) {
		(line == Line.TOP? topToolBar: botToolBar).setVisible(false);
		(line == Line.TOP? topToolBar: botToolBar).setManaged(false);
	}

	public void removeItem(Line line, int pos) {
		actions.add(() -> {
			(line == Line.TOP? topToolBar: botToolBar).getItems().remove(pos);
		});
	}

	public void moveToolbarItem(Line srcLine, int srcPos, Line tgtLine, int tgtPos) {
		actions.add(() -> {
			final Node node = (srcLine == Line.TOP? topToolBar: botToolBar).getItems().get(srcPos);
			if (srcLine != tgtLine) {
				(srcLine == Line.TOP? topToolBar: botToolBar).getItems().remove(node);
			}
			(tgtLine == Line.TOP? topToolBar: botToolBar).getItems().add(tgtPos, node);
		});
	}

	public void doWhenReady() {
		ThreadPool.getDefault().mustRun("clean HTML editor toolbar", () -> {
			try { countDownLatch.await(); }
			catch (final InterruptedException e) { LOGGER.error("waiting for toolbars...", e); }

			Platform.runLater(() -> actions.forEach(a -> a.run()));
		});
	}
}
