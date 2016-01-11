package net.anfoya.mail.browser.javafx;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class FixedSplitPane extends HBox {
	
	private ObservableList<Pane> panes;
	private Pane resizable;
	
	public FixedSplitPane() {
		panes = FXCollections.observableArrayList();
		panes.addListener((Change<? extends Pane> c) -> handleChange(c));
	}
	
	private void handleChange(Change<? extends Pane> c) {
		getChildren().clear();
		panes.forEach(p -> {
			if (!getChildren().isEmpty()) {
				addDivider();
			}
			getChildren().add(p);
		});
		setResizableWithParent(resizable);
		panes.forEach(p -> p.requestLayout());
	}

	private void addDivider() {
		Label label = new Label();
		label.prefHeightProperty().bind(heightProperty());
		label.setMinWidth(1);
		label.setMaxWidth(1);
		label.setStyle("-fx-background-color: black");
		label.setCursor(Cursor.H_RESIZE);
		getChildren().add(label);
	}

	public ObservableList<Pane> getPanes() {
		return panes;
	}

	public void setDividerPositions(double... positions) {
		double accumulator = 0;
		for(int i=0, n=Math.min(positions.length, getPanes().size()); i<n ; i++) {
			getPanes().get(i).setMinWidth(positions[i] - accumulator);
			getPanes().get(i).setMaxWidth(positions[i] - accumulator);
			accumulator += positions[i];
		}
	}
	
	public void setResizableWithParent(Pane pane) {
		resizable = pane;
		getPanes().forEach(p -> HBox.setHgrow(p, p == pane? Priority.ALWAYS: Priority.NEVER));
	}

	public static double getDividerWidth() {
		return 1;
	}
}
