package net.anfoya.tag.javafx.scene.tag;

import javafx.application.Platform;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import net.anfoya.javafx.scene.control.IncExcListCell;
import net.anfoya.tag.javafx.scene.dnd.ExtItemDropPane;
import net.anfoya.tag.service.Tag;

class TagListCell<T extends Tag> extends IncExcListCell<TagListItem<T>> {
	public TagListCell(final boolean withExcludeBox) {
		super(withExcludeBox);

		setOnDragDetected(e -> {
			if (getItem() != null && !getItem().getTag().isSystem()) {
		        final ClipboardContent content = new ClipboardContent();
		        content.put(Tag.TAG_DATA_FORMAT, getItem().getTag());
		        final Dragboard db = startDragAndDrop(TransferMode.ANY);
		        db.setContent(content);
		        e.consume();
			}
		});
		setOnDragOver(e -> {
			if (getItem() != null && e.getDragboard().hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT)) {
		        e.acceptTransferModes(TransferMode.ANY);
			}
			e.consume();
		});
		setOnDragEntered(e -> {
			if (getItem() != null && e.getDragboard().hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT)) {
				Platform.runLater(() -> getStyleClass().add("tag-list-cell-dnd-highlight"));
			}
		});
		setOnDragExited(e -> Platform.runLater(() -> getStyleClass().remove("tag-list-cell-dnd-highlight")));
		setOnDragDropped(event -> {
			final Dragboard db = event.getDragboard();
			if (getItem() != null && db.hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT)) {
				final ClipboardContent content = new ClipboardContent();
				content.put(Tag.TAG_DATA_FORMAT, getItem().getTag());
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