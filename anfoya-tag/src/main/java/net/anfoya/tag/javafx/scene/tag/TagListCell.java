package net.anfoya.tag.javafx.scene.tag;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import net.anfoya.javafx.scene.control.IncExcListCell;
import net.anfoya.javafx.scene.dnd.DndHelper;
import net.anfoya.tag.javafx.scene.dnd.ExtItemDropPane;
import net.anfoya.tag.service.Tag;

class TagListCell<T extends Tag> extends IncExcListCell<TagListItem<T>> {
	public TagListCell(final boolean withExcludeBox) {
		super(withExcludeBox);

		setOnDragDetected(e -> {
			if (getItem() != null) {
		        final ClipboardContent content = new ClipboardContent();
		        content.put(Tag.TAG_DATA_FORMAT, getItem().getTag());

		        final String name = getItem().getTag().getName();
		        final Image image = new DndHelper(getScene().getStylesheets()).textToImage(name);

		        final Dragboard db = startDragAndDrop(TransferMode.LINK);
		        db.setContent(content);
		        db.setDragView(image, image.getWidth() / 2, image.getHeight() / 2);
		        e.consume();
			}
		});
		setOnDragOver(e -> {
			if (getItem() != null && e.getDragboard().hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT)) {
		        e.acceptTransferModes(TransferMode.LINK);
			}
		});
		setOnDragEntered(e -> {
			if (getItem() != null && e.getDragboard().hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT)) {
				Platform.runLater(() -> getStyleClass().add("tag-list-cell-dnd-highlight"));
			}
		});
		setOnDragExited(e -> Platform.runLater(() -> getStyleClass().remove("tag-list-cell-dnd-highlight")));
		setOnDragDropped(e -> {
			final Dragboard db = e.getDragboard();
			if (getItem() != null && db.hasContent(ExtItemDropPane.ADD_TAG_DATA_FORMAT)) {
				final ClipboardContent content = new ClipboardContent();
				content.put(Tag.TAG_DATA_FORMAT, getItem().getTag());
				db.setContent(content);
				e.setDropCompleted(true);
				e.consume();
			}
		});
	}

	@Override
	public void updateItem(TagListItem<T> item, boolean empty) {
		super.updateItem(item, empty);
		if (!empty && item != null && item.getTag().isSystem()) {
			Platform.runLater(() -> getStyleClass().add("system"));
		} else {
			Platform.runLater(() -> getStyleClass().remove("system"));
		}
	}
}