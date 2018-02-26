package net.anfoya.javafx.scene.layout;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class FixedSplitPane extends HBox {
	private class Divider extends Region {
		private final int index;
		public Divider(int index) {
			this.index = index;
			getStyleClass().add("split-pane-divider");
			setCursor(Cursor.H_RESIZE);
			prefHeightProperty().bind(FixedSplitPane.this.heightProperty());
			setOnMouseDragged(e -> moveDiv(e));
		}
		public int getIndex() {
			return index;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(FixedSplitPane.class);
	private static final double PANE_MIN_WIDTH = 50;
	private final ObservableList<Pane> panes;
	private final Map<Pane, Double> paneWidths;

	private Pane resizable;
	private double dividerWidth;

	public FixedSplitPane() {
		getStyleClass().add("fixed-split-pane");
		widthProperty().addListener((ov, o, n) -> refreshWidths());

		panes = FXCollections.observableArrayList();
		panes.addListener((Change<? extends Pane> c) -> updateChildren());

		paneWidths = new HashMap<Pane, Double>();

		dividerWidth = 0;
		resizable = null;
	}

	private void updateChildren() {
		getChildren().clear();
		final AtomicInteger index = new AtomicInteger(0);
		panes.forEach(p -> {
			if (index.get() > 0) {
				getChildren().add(new Divider(index.get()-1));
			}
			index.incrementAndGet();

			paneWidths.put(p, p.getPrefWidth());
			p.managedProperty().bind(p.visibleProperty());
			getChildren().add(p);
		});
	}

	private void moveDiv(MouseEvent event) {
		if (!event.isPrimaryButtonDown()) {
			return;
		}

		final int index = ((Divider)event.getSource()).getIndex();
		final Pane previous = getPreviousVisible(index);
		if (previous == null) {
			return;
		}
		final Pane next = getNextVisible(index+1);
		if (next == null) {
			return;
		}

		final double maxWidth = previous.getWidth() + next.getWidth() - PANE_MIN_WIDTH;
		paneWidths.put(previous, Math.min(maxWidth, Math.max(PANE_MIN_WIDTH, previous.getWidth() + event.getX())));
		paneWidths.put(next, Math.min(maxWidth, Math.max(PANE_MIN_WIDTH, next.getWidth() - event.getX())));
		refreshWidths();

		event.consume();
	}

	private Pane getPreviousVisible(int index) {
		Pane previous;
		for(previous=panes.get(index); index>0 && !panes.get(index).isVisible(); previous=panes.get(--index));
		return previous != null && previous.isVisible()? previous: null;
	}

	private Pane getNextVisible(int index) {
		Pane next;
		for(next=panes.get(index); index<panes.size()-1 && !panes.get(index).isVisible(); next=panes.get(++index));
		return next != null && next.isVisible()? next: null;
	}

	private void setPaneWidth(Pane pane, double width) {
		LOGGER.debug("setPaneWidth {} {}", pane.getClass().getName(), width);
		pane.setPrefWidth(width);
		if (pane == resizable) {
			pane.setMinWidth(PANE_MIN_WIDTH);
			pane.setMaxWidth(Double.MAX_VALUE);
		} else {
			pane.setMinWidth(width);
			pane.setMaxWidth(width);
		}
	}

	private void refreshWidths() {
		if (resizable != null
				&& resizable.isVisible()
				&& panes.contains(resizable)) {
			paneWidths.put(resizable, resizable.getWidth());
		}
		panes.forEach(pane -> setPaneWidth(pane, paneWidths.get(pane)));
	}

	public double computePrefWidth() {
		double prefWidth = getDividerWidth() * (getChildren().size() - getPanes().size());
		for(final Pane p: panes) {
			if (p.isVisible()) {
				prefWidth += paneWidths.get(p);
			}
		}

		LOGGER.debug("prefWidth {}", prefWidth);

		return prefWidth;
	}

	public double computeMinWidth() {
		double minWidth = getDividerWidth() * (getChildren().size() - getPanes().size());
		Pane lastVisiblePane = null;
		for(final Pane p: panes) {
			if (p.isVisible()) {
				minWidth += paneWidths.get(p);
				lastVisiblePane = p;
			}
		}
		minWidth += PANE_MIN_WIDTH;
		minWidth -= paneWidths.get(lastVisiblePane);

		LOGGER.debug("prefWidth {}", minWidth);

		return minWidth;
	}

	public ObservableList<Pane> getPanes() {
		return panes;
	}

	public void setVisiblePanes(Pane... panes) {
		final List<Pane> hidden = new ArrayList<Pane>(getPanes());
		Arrays.stream(panes).forEach(p -> {
			p.setVisible(true);
			hidden.remove(p);
		});
		hidden.forEach(p -> p.setVisible(false));
	}

	public List<Pane> getVisiblePanes() {
		return getPanes().stream().filter(p -> p.isVisible()).collect(Collectors.toList());
	}

	public void setResizableWithParent(Pane pane) {
		if (resizable == pane) {
			return;
		}

		if (resizable != null) {
			resizable.setMinWidth(resizable.getWidth());
			resizable.setMaxWidth(resizable.getWidth());
			paneWidths.put(resizable, resizable.getWidth());
		}
		resizable = pane;
		getChildren().forEach(p -> HBox.setHgrow(p, p == resizable? Priority.ALWAYS: null));
		if (resizable != null) {
			resizable.setMinWidth(Pane.USE_COMPUTED_SIZE);
			resizable.setMaxWidth(Double.MAX_VALUE);
		}
	}

	public double getDividerWidth() {
		if (dividerWidth == 0 && getPanes().size() > 1) {
			dividerWidth = ((Divider)getChildren().get(1)).getWidth();
		}
		return dividerWidth;
	}
}
