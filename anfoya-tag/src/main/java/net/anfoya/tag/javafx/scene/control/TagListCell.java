package net.anfoya.tag.javafx.scene.control;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import net.anfoya.javafx.scene.control.ExcludeBox;
import net.anfoya.tag.model.SimpleTag;

class TagListCell<T extends SimpleTag> extends CheckBoxListCell<TagListItem<T>> {
	public TagListCell() {
		this(null);
	}

	public TagListCell(final DataFormat dropDataFormat) {
		super();
		setSelectedStateCallback(new Callback<TagListItem<T>, ObservableValue<Boolean>>() {
			@Override
			public ObservableValue<Boolean> call(final TagListItem<T> item) {
				return item.includedProperty();
			}
		});
		if (dropDataFormat != null) {
	        setOnDragEntered(event -> {
				if (getItem() != null && event.getDragboard().hasContent(dropDataFormat)) {
		            setOpacity(0.3);
		            event.consume();
	        	}
	        });
	        setOnDragExited(event -> {
				if (getItem() != null && event.getDragboard().hasContent(dropDataFormat)) {
		            setOpacity(1);
		            event.consume();
	        	}
	        });
			setOnDragOver(event -> {
				if (getItem() != null && event.getDragboard().hasContent(dropDataFormat)) {
					event.acceptTransferModes(TransferMode.ANY);
					event.consume();
				}
			});
			setOnDragDropped(event -> {
				final Dragboard db = event.getDragboard();
				if (getItem() != null && db.hasContent(dropDataFormat)) {
					final ClipboardContent content = new ClipboardContent();
					content.put(dropDataFormat, db.getContent(dropDataFormat));
					content.put(SectionListPane.DND_TAG_DATA_FORMAT, getItem().getTag());
					db.setContent(content);
				}
			});
		}
	}

	@Override
    public void updateItem(final TagListItem<T> item, final boolean empty) {
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
        	//setDisable(item.disableProperty().get());
	        setTextFill(isDisabled()? Color.GRAY: Color.BLACK);
        }
	}
}