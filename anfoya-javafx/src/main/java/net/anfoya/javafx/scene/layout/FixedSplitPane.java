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
			if (p != resizable) {
				p.setMinWidth(p.getPrefWidth());
				p.setMaxWidth(p.getPrefWidth());
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
		Pane before = null;
		for(int i = index; i>=0; i--) {
			if (panes.get(i).isVisible()) {
				before = panes.get(i);
				break;
			}
		}
		if (before == null) {
			return;
		}
		Pane after = null;
		for(int i = index+1; i<panes.size(); i++) {
			if (panes.get(i).isVisible()) {
				after = panes.get(i);
				break;
			}
		}
		if (after == null) {
			return;
		}

		double minWidth = 50;
		double maxWidth = before.getWidth() + after.getWidth() - minWidth;
		
		double widthBefore = Math.min(maxWidth, Math.max(minWidth, before.getWidth() + event.getX()));
		double widthAfter = Math.min(maxWidth, Math.max(minWidth, after.getWidth() - event.getX()));
		
		setPaneWidth(before, widthBefore);
		setPaneWidth(after, widthAfter);
		
		event.consume();
	}
	
	private void setPaneWidth(Pane pane, double width) {
		if (pane == resizable) {
			pane.setPrefWidth(width);
		} else {
			pane.setMinWidth(width);
			pane.setMaxWidth(width);
		}
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
		getPanes().forEach(p -> setPaneWidth(p, p.getPrefWidth()));
	}

	public List<Pane> getVisiblePanes() {
		return getPanes().parallelStream().filter(p -> p.isVisible()).collect(Collectors.toList());
	}

	public void setResizableWithParent(Pane pane) {
		if (resizable == pane) {
			return;
		}
		
		if (resizable != null) {
			resizable.setMinWidth(resizable.getWidth());
			resizable.setMaxWidth(resizable.getWidth());
		}
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
