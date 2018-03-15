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
	private static final int MAX_RENDERING_PIXELS = 8192;

	private final WebView view;
	private final WebEngine engine;

	public WebViewFitContent() {
		view = new WebView();
		view.focusTraversableProperty().bind(focusTraversableProperty());
		view.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
			@Override
			public void onChanged(final Change<? extends Node> change) {
				final Set<Node> scrolls = view.lookupAll(".scroll-bar");
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
		getChildren().add(view);

		engine = view.getEngine();

		widthProperty().addListener((ov, o, n) -> {
			LOGGER.debug("widthProperty() change");
			view.setPrefWidth(Math.min(MAX_RENDERING_PIXELS, n.intValue()));
			adjustHeight();
		});

	}

	@Override
	protected void layoutChildren() {
		final double w = getWidth();
		final double h = getHeight();
		layoutInArea(view, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
	}

	private void adjustHeight() {
		try {
			final int height = getVscrollMax();
			if (height != 0) {
				view.setPrefHeight(height);
				view.getPrefHeight();

				LOGGER.debug("{}", height);
			}
		} catch (final JSException e) {
			e.printStackTrace();
		}
	}

	public void setContextMenuEnabled(final boolean enable) {
		view.setContextMenuEnabled(enable);
	}

	public void setScrollHandler(final EventHandler<ScrollEvent> handler) {
		final EventDispatcher eventDispatcher = getEventDispatcher();
		setEventDispatcher((e, tail) -> {
	    	if (e.getEventType() == ScrollEvent.SCROLL) {
	    		final ScrollEvent event = (ScrollEvent) e;
	    		if (event.getDeltaY() != 0) {
		    		handler.handle(event);
		    		e = new ScrollEvent(
		    				event.getSource(), event.getTarget(), event.getEventType()
		    				, event.getX(), event.getY(), event.getScreenX(), event.getSceneY()
		    				, event.isShiftDown(), event.isControlDown(), event.isAltDown(), event.isMetaDown()
		    				, event.isDirect(), event.isInertia()
		    				, event.getDeltaX(), 0, event.getTotalDeltaX(), 0
		    				, event.getTextDeltaXUnits(), event.getTextDeltaX(), event.getTextDeltaYUnits(), 0
		    				, event.getTouchCount(), event.getPickResult());
	    		}
	    	}
			return eventDispatcher.dispatchEvent(e, tail);
		});
	}

	public int getVscrollMax() {
		return Math.min(
				MAX_RENDERING_PIXELS
				, (int) engine.executeScript("document.body.scrollHeight - document.body.scrollTop"));
	}

	public WebEngine getEngine() {
		return view.getEngine();
	}

	public WebView getWebView() {
		return view;
	}
}
