package net.anfoya.movie.browser.javafx.taglist;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import net.anfoya.javafx.scene.control.ExcludeBox;

class TagListCell extends CheckBoxListCell<TagListItem> {
	public TagListCell() {
		super();
		setSelectedStateCallback(new Callback<TagListItem, ObservableValue<Boolean>>() {
			@Override
			public ObservableValue<Boolean> call(final TagListItem item) {
				return item.includedProperty();
			}
		});
	}

	@Override
    public void updateItem(final TagListItem item, final boolean empty) {
        super.updateItem(item, empty);

    	if (item == null || empty) {
            setGraphic(null);
        } else {
	        final ExcludeBox excludeBox = new ExcludeBox();
	        excludeBox.setExcluded(item.excludedProperty().get());
	        excludeBox.excludedProperty().addListener(new ChangeListener<Boolean>() {
	        	@Override
	        	public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
	        		item.excludedProperty().set(newVal);
	        	}
			});

        	final BorderPane pane = new BorderPane();
        	pane.setCenter(getGraphic());
        	pane.setRight(excludeBox);

        	setGraphic(pane);
        	setDisable(item.disableProperty().get());
	        setTextFill(isDisabled()? Color.GRAY: Color.BLACK);
        }
	}
}