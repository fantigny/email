package net.anfoya.javafx.scene.web;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ListChangeListener;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;

public final class WebViewFitContent extends Region {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebViewFitContent.class);

	private final WebView delegate;

	public WebViewFitContent() {
		delegate = new WebView();

		widthProperty().addListener((ov, oldVal, newVal) -> {
			LOGGER.debug("widthProperty() change");
			delegate.setPrefWidth((Double) newVal);
			adjustHeight();
		});
		delegate.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
			@Override
			public void onChanged(final Change<? extends Node> change) {
				final Set<Node> scrolls = delegate.lookupAll(".scroll-bar");
				for (final Node node : scrolls) {
					final ScrollBar scrollBar = (ScrollBar) node;
					if (scrollBar.isVisible() && scrollBar.getOrientation() == Orientation.VERTICAL) {
						node.setVisible(false);
						LOGGER.debug("getChildrenUnmodifiable() change");
						adjustHeight();
					}
				}
			}
		});

		getChildren().add(delegate);
	}

	@Override
	protected void layoutChildren() {
		final double w = getWidth();
		final double h = getHeight();
		layoutInArea(delegate, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
	}

	private void adjustHeight() {
		try {
			final int height = getVscrollMax();
			if (height != 0) {
				delegate.setPrefHeight(height);
				delegate.getPrefHeight();

				LOGGER.debug("{}", height);
			}
		} catch (final JSException e) {
			e.printStackTrace();
		}
	}

	public void setContextMenuEnabled(final boolean enable) {
		delegate.setContextMenuEnabled(enable);
	}

	public void setScrollHandler(final EventHandler<ScrollEvent> handler) {
		final EventDispatcher eventDispatcher = getEventDispatcher();
		setEventDispatcher((event, tail) -> {
	    	if (event.getEventType() == ScrollEvent.SCROLL) {
	    		handler.handle((ScrollEvent) event);
	    		return null;
	    	}
			return eventDispatcher.dispatchEvent(event, tail);
		});
	}

	public int getVscrollMax() {
		return Math.min(8192, (int) delegate.getEngine().executeScript("document.body.scrollHeight - document.body.scrollTop"));
	}

	public WebEngine getEngine() {
		return delegate.getEngine();
	}
}
