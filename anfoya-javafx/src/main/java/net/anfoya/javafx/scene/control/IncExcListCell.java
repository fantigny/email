package net.anfoya.javafx.scene.control;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;

public class IncExcListCell<I extends IncExcListItem> extends ListCell<I> {
	private final boolean showExcludeBox;

	public IncExcListCell(final boolean showExcludeBox) {
		super();
		this.showExcludeBox = showExcludeBox;
	}

	@Override
    public void updateItem(final I item, final boolean empty) {
        super.updateItem(item, empty);

    	if (empty || item == null) {
            setFocusTraversable(false);
            setText("");
            setGraphic(null);
        } else {
            setFocusTraversable(item.focusTraversableProperty().get());
        	setText(item.textProperty().get());

            final CheckBox checkBox = new CheckBox();
            checkBox.setFocusTraversable(isFocusTraversable());
            checkBox.setSelected(item.includedProperty().get());
            checkBox.selectedProperty().addListener((ov, oldVal, newVal) -> item.includedProperty().set(newVal));
	        setGraphic(checkBox);

	        if (showExcludeBox) {
	            final ExcludeBox excludeBox = new ExcludeBox();
	            excludeBox.setFocusTraversable(isFocusTraversable());
		        excludeBox.setExcluded(item.excludedProperty().get());
		        excludeBox.excludedProperty().addListener((ov, oldVal, newVal) -> item.excludedProperty().set(newVal));
		        checkBox.setGraphic(excludeBox);
	        }

	        item.textProperty().addListener((ov, oldVal, newVal) -> updateItem(item, empty));
	        item.focusTraversableProperty().addListener((ov, o, n) -> setFocusTraversable(n));

        	//TODO: setDisable(item.disableProperty().get());
        }
	}
}