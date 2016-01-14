package net.anfoya.javafx.scene.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
	private final ObservableList<Pane> panes;

	private Pane resizable;
	private double dividerWidth;

	public FixedSplitPane() {
		getStyleClass().add("fixed-split-pane");

		panes = FXCollections.observableArrayList();
		panes.addListener((Change<? extends Pane> c) -> updateChildren());

		dividerWidth = 0;
		resizable = null;
	}

	private void updateChildren() {
		getChildren().clear();
		AtomicInteger index = new AtomicInteger(0);
		panes.forEach(p -> {
			if (index.getAndIncrement() > 0) {
				getChildren().add(new Divider(index.get()-2));
			}
			p.managedProperty().bind(p.visibleProperty());
			getChildren().add(p);
		});
	}

	private void moveDiv(MouseEvent event) {
		if (!event.isPrimaryButtonDown()) {
			return;
		}
		final int index = ((Divider)event.getSource()).getIndex();
		final double[] positions = getDividerPositions();
		final double minPos = 50 + (index == 0? 0: positions[index-1]);
		final double maxPos = (index == positions.length-1? getScene().getWindow().getWidth(): positions[index+1]) - 50;
		final double pos = Math.min(maxPos, Math.max(minPos, positions[index] + event.getX()));
		setDividerPosition(index, pos);
		event.consume();
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
		if (resizable != null && !resizable.isVisible()) {
			resizable = null;
		}
		setDividerPositions(getDividerPositions());
	}

	public List<Pane> getVisiblePanes() {
		return getPanes().parallelStream().filter(p -> p.isVisible()).collect(Collectors.toList());
	}

	public void setDividerPositions(double... positions) {
		if (positions.length < getPanes().size() - 1) {
			final double[] partial = positions;
			positions = getDividerPositions();
			for(int i=0; i<=partial.length ; i++) {
				positions[i] = partial[i];
			}
		}

		final double[] widths = new double[positions.length+1];
		double accumulator = 0;
		for(int i=0; i<positions.length; i++) {
			final Pane pane = getPanes().get(i);
			if (pane.isVisible()) {
				widths[i] = positions[i] - accumulator;
			} else {
				widths[i] = 0;
			}
			accumulator += widths[i];
		}
		widths[positions.length] = getWidth() == 0? 0: getWidth() - accumulator;

		final Pane resizable = getResizableWithParent();
		for(int i=0; i<widths.length ; i++) {
			final double width = widths[i];
			if (width != 0) {
				final Pane pane = getPanes().get(i);
				pane.setPrefWidth(width);
				if (pane != resizable) {
					pane.setMinWidth(width);
					pane.setMaxWidth(width);
				}
			}
		}
	}

	public void setDividerPosition(int divIndex, double position) {
		final double[] positions = getDividerPositions();
		positions[divIndex] = position;
		setDividerPositions(positions);
	}

	public double[] getDividerPositions() {
		double accumulator = 0;
		final double[] positions = new double[panes.size() - 1];
		for(int i=0; i<positions.length; i++) {
			positions[i] = accumulator + panes.get(i).getWidth();
			accumulator += positions[i];
		}
		return positions;
	}

	private Pane getResizableWithParent() {
		Pane resizable = this.resizable;
		if (resizable == null) {
			resizable = getPanes().get(getPanes().size() - 1);
			resizable.setMinWidth(Pane.USE_COMPUTED_SIZE);
			resizable.setMaxWidth(Double.MAX_VALUE);
			resizable.requestLayout();
		}
		return resizable;
	}

	public void setResizableWithParent(Pane pane) {
		resizable = pane;
		getChildren().forEach(p -> HBox.setHgrow(p, p == resizable? Priority.ALWAYS: null));
		if (resizable != null) {
			resizable.setMinWidth(Pane.USE_COMPUTED_SIZE);
			resizable.setMaxWidth(Double.MAX_VALUE);
			resizable.requestLayout();
		}
	}

	public double getDividerWidth() {
		if (dividerWidth == 0 && getPanes().size() > 1) {
			dividerWidth = ((Divider)getChildren().get(1)).getWidth();
		}
		return dividerWidth;
	}

	public double computeSize() {
		return getChildren()
				.parallelStream()
				.filter(n -> n.isVisible())
				.mapToDouble(n -> n instanceof Divider? getDividerWidth(): ((Pane) n).getWidth())
				.sum();
	}
}
