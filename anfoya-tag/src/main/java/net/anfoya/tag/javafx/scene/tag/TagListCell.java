package net.anfoya.tag.javafx.scene.tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import net.anfoya.javafx.scene.control.ExcludeBox;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.service.Tag;

class TagListCell<T extends Tag> extends ListCell<TagListItem<T>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagListCell.class);
	private boolean showExcludeBox;

	public TagListCell(final boolean showExcludeBox) {
		super();
		this.showExcludeBox = showExcludeBox;
//		setSelectedStateCallback(item -> item.includedProperty());
	}

	public TagListCell(final DataFormat dataFormat, final boolean withExcludeBox) {
		this(withExcludeBox);

		setOnDragDetected(event -> {
			if (getItem() != null && !getItem().getTag().isSystem()) {
		        final ClipboardContent content = new ClipboardContent();
		        content.put(DndFormat.TAG_DATA_FORMAT, getItem().getTag());
		        final Dragboard db = startDragAndDrop(TransferMode.ANY);
		        db.setContent(content);
		        event.consume();
			}
		});
		setOnDragOver(event -> {
			if (getItem() != null && event.getDragboard().hasContent(dataFormat)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
        setOnDragEntered(event -> {
        	LOGGER.debug("{}", dataFormat);
			if (getItem() != null && event.getDragboard().hasContent(dataFormat)) {
				event.acceptTransferModes(TransferMode.ANY);
	            setOpacity(0.5);
	            event.consume();
        	}
        });
        setOnDragExited(event -> {
			if (getItem() != null && event.getDragboard().hasContent(dataFormat)) {
	            setOpacity(1);
	            event.consume();
        	}
        });
		setOnDragDropped(event -> {
			final Dragboard db = event.getDragboard();
			if (getItem() != null && db.hasContent(dataFormat)) {
				final ClipboardContent content = new ClipboardContent();
				content.put(dataFormat, db.getContent(dataFormat));
				content.put(DndFormat.TAG_DATA_FORMAT, getItem().getTag());
				db.setContent(content);
				event.setDropCompleted(true);
				event.consume();
			}
		});
	}

	@Override
    public void updateItem(final TagListItem<T> item, final boolean empty) {
        super.updateItem(item, empty);

    	if (item == null || empty) {
            setText("");
            setGraphic(null);
        } else {
            setFocusTraversable(item.focusTraversableProperty().get());
            setTextFill(item.getTag().isSystem()? Color.DARKBLUE: Color.BLACK);
        	setText(item.toString());

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

        	//TODO: setDisable(item.disableProperty().get());
        }
	}
}