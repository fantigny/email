package net.anfoya.javafx.scene.web;

import java.util.Set;

import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;

public final class WebViewFitContent extends Region {
	private final WebView webview;
	private final WebEngine webEngine;

	public WebViewFitContent(final ScrollPane parentScrollPane) {
		webview = new WebView();
		webEngine = webview.getEngine();

		widthProperty().addListener((ov, oldVal, newVal) -> {
			webview.setPrefWidth((Double) newVal);
			adjustHeight();
		});
		webEngine.getLoadWorker().stateProperty().addListener((ov, oldVal, newVal) -> {
			adjustHeight();
		});
		webview.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
			@Override
			public void onChanged(final Change<? extends Node> change) {
				final Set<Node> scrolls = webview.lookupAll(".scroll-bar");
				for (final Node n : scrolls) {
					n.setVisible(false);
				}
			}
		});
		parentScrollPane.setEventDispatcher((event, tail) -> {
	    	if (event.getEventType() == ScrollEvent.SCROLL) {
	    		final double current = parentScrollPane.getVvalue();
	    		final double offset = ((ScrollEvent)event).getDeltaY() > 0? .1: -.1;
		    	parentScrollPane.setVvalue(current + offset);
	    		event.consume();
	    		return null;
	    	}
			return tail.dispatchEvent(event);
		});

		getChildren().add(webview);
	}

	public void loadContent(final String html) {
		webEngine.loadContent(html);
	}

	public void load(final String location) {
		webEngine.load(location);
	}

	@Override
	protected void layoutChildren() {
		final double w = getWidth();
		final double h = getHeight();
		layoutInArea(webview, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
	}

	private void adjustHeight() {
		try {
			final Object result = webEngine.executeScript("Math.max("
					+ "document.body.scrollHeight"
					+ ", document.body.offsetHeight"
					+ ", document.documentElement.clientHeight"
					+ ", document.documentElement.scrollHeight"
					+ ", document.documentElement.offsetHeight )");
			if (result instanceof Integer) {
				webview.setMinHeight(Math.max(getMinHeight(), 20.0 + (Integer) result));
				webview.getPrefHeight();
			}
		} catch (final JSException e) {
			e.printStackTrace();
		}
	}

	public void setContextMenuEnabled(final boolean enable) {
		webview.setContextMenuEnabled(enable);
	}

	public void clear() {
		loadContent("");
	}
}
