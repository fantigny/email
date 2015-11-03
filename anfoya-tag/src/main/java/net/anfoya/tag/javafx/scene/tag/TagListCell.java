package net.anfoya.tag.javafx.scene.tag;

import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import net.anfoya.javafx.scene.control.IncExcListCell;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.service.Tag;

class TagListCell<T extends Tag> extends IncExcListCell<TagListItem<T>> {

	public TagListCell(final boolean withExcludeBox) {
		super(withExcludeBox);
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
				setOpacity(.7);
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		setOnDragExited(e -> {
			setOpacity(1);
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
	public void updateItem(TagListItem<T> item, boolean empty) {
		super.updateItem(item, empty);
		if (!empty && item != null) {
			setTextFill(item.getTag().isSystem()? Color.DARKBLUE: Color.BLACK);
		}
	}
}