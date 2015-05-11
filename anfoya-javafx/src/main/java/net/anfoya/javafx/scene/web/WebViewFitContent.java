package net.anfoya.javafx.scene.web;

import java.util.Set;

import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebViewFitContent extends Region {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebViewFitContent.class);

	private final WebView delegate;

	private double scrollHeight;

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
				for (final Node n : scrolls) {
					LOGGER.debug("{}", n);
					if (n.isVisible()) {
						LOGGER.debug("getChildrenUnmodifiable() change");
						adjustHeight();
					}
				}
			}
		});

		getChildren().add(delegate);
	}

	public void loadContent(final String html) {
		delegate.getEngine().loadContent(html);
	}

	public void load(final String location) {
		delegate.getEngine().load(location);
	}

	@Override
	protected void layoutChildren() {
		final double w = getWidth();
		final double h = getHeight();
		layoutInArea(delegate, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
	}

	private void adjustHeight() {
		if (getParent() == null) {
			return;
		}
		try {
			final double scrollHeight = (int) delegate.getEngine().executeScript("document.body.scrollHeight");
			if (scrollHeight != 0) {
				this.scrollHeight = scrollHeight;
				delegate.setPrefHeight(scrollHeight);
				delegate.getPrefHeight();

				LOGGER.debug("{}", scrollHeight);
			}
		} catch (final JSException e) {
			e.printStackTrace();
		}
	}

	public void setContextMenuEnabled(final boolean enable) {
		delegate.setContextMenuEnabled(enable);
	}

	public void clear() {
		loadContent("");
	}

	public void setParentScrollPane(final ScrollPane parentScrollPane) {
		parentScrollPane.setEventDispatcher((event, tail) -> {
	    	if (event.getEventType() == ScrollEvent.SCROLL) {
	    		final ScrollEvent scrollEvent = (ScrollEvent) event;
	    		final double current = parentScrollPane.getVvalue();
	    		final double offset = scrollEvent.getDeltaY() / scrollHeight * -1;
		    	parentScrollPane.setVvalue(current + offset);
	    		event.consume();
	    		return null;
	    	}
			return tail.dispatchEvent(event);
		});
	}
}
