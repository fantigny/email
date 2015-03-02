package net.anfoya.movies.javafx.taglist;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.RadioButton;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;

class TagListCell extends CheckBoxListCell<TagListItem> {
	public TagListCell() {
		super();
		setSelectedStateCallback(new Callback<TagListItem, ObservableValue<Boolean>>() {
			@Override
			public ObservableValue<Boolean> call(final TagListItem item) {
				return item.selectedProperty();
			}
		});
	}

	@Override
    public void updateItem(final TagListItem item, final boolean empty) {
        super.updateItem(item, empty);

    	if (item == null || empty) {
            setGraphic(null);
        } else {
        	setDisable(item.isDisable() && !item.isExcluded());
	        setTextFill(isDisabled()? Color.GRAY: Color.BLACK);

	        final boolean excludeDisable;
	        if (item.isExcluded()) {
	        	excludeDisable = false;
	        } else {
	        	excludeDisable = item.isDisable() || item.isSelected();
	        }

	        final RadioButton excludeButton = new RadioButton();
	        excludeButton.setDisable(excludeDisable);
	        excludeButton.setSelected(item.isExcluded() && !item.isSelected() &&!excludeDisable);
	        item.excludedProperty().bind(excludeButton.selectedProperty());

        	final BorderPane pane = new BorderPane();
        	pane.setCenter(getGraphic());
        	pane.setRight(excludeButton);

        	setGraphic(pane);
        }
	}
}