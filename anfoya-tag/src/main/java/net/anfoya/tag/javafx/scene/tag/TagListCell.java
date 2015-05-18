package net.anfoya.tag.javafx.scene.tag;

import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import net.anfoya.javafx.scene.control.ExcludeBox;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TagListCell<T extends SimpleTag> extends CheckBoxListCell<TagListItem<T>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagListCell.class);

	public TagListCell() {
		super();
		setSelectedStateCallback(item -> item.includedProperty());
	}

	public TagListCell(final DataFormat dataFormat) {
		this();

		setOnDragDetected(event -> {
			if (getItem() != null) {
		        final ClipboardContent content = new ClipboardContent();
		        content.put(DndFormat.TAG_DATA_FORMAT, getItem().getTag());
		        final Dragboard db = startDragAndDrop(TransferMode.ANY);
		        db.setContent(content);
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
            setGraphic(null);
        } else {
	        final ExcludeBox excludeBox = new ExcludeBox();
	        excludeBox.setExcluded(item.excludedProperty().get());
	        excludeBox.excludedProperty().addListener((ov, oldVal, newVal) -> item.excludedProperty().set(newVal));

        	final BorderPane pane = new BorderPane();
        	pane.setCenter(getGraphic());
        	pane.setRight(excludeBox);

	        item.textProperty().addListener((ov, oldVal, newVal) -> updateItem(item, empty));

        	setGraphic(pane);
        	//TODO: setDisable(item.disableProperty().get());
	        setTextFill(isDisabled()? Color.GRAY: Color.BLACK);
        }
	}
}