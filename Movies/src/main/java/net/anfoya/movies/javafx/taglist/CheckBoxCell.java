package net.anfoya.movies.javafx.taglist;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.paint.Color;
import javafx.util.Callback;

class CheckBoxCell extends CheckBoxListCell<TagListItem> {
	public CheckBoxCell() {
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
        if (item == null) {
        	return;
        }
        setDisable(item.isDisable());
        setTextFill(isDisabled()? Color.GRAY: Color.BLACK);
	}
}