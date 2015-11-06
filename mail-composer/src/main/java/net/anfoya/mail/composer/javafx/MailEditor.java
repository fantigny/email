package net.anfoya.mail.composer.javafx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import net.anfoya.javafx.scene.control.HtmlEditorListener;
import net.anfoya.javafx.scene.control.HtmlEditorToolBarHelper;
import net.anfoya.javafx.scene.control.HtmlEditorToolBarHelper.Line;
import net.anfoya.mail.browser.javafx.util.UrlHelper;

public class MailEditor extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailEditor.class);
    private static final Image ATTACHMENT = new Image(MailEditor.class.getResourceAsStream("/net/anfoya/mail/image/attachment.png"));
    private static final DataFormat DND_REMOVE_FILE_DATA_FORMAT = new DataFormat("DND_REMOVE_FILE_DATA_FORMAT");

	private final HTMLEditor editor;
	private final WebView editorView;
	private final BooleanProperty editedProperty;

	private final FlowPane attachmentPane;
	private final Set<File> attachments;

	private final StackPane removeAttachPane;

	private Callback<String, Void> onMailtoCallback;

	public MailEditor() {
		removeAttachPane = new StackPane();

		editor = new HTMLEditor();
		editor.needsLayoutProperty().addListener((ov, o, n) -> cleanToolBar());
		editor.setStyle("-fx-background-color: transparent; -fx-border-width: 0 0 1 0; -fx-border-color: lightgray; -fx-font-size: 11px;");
		setCenter(editor);

		editedProperty = new SimpleBooleanProperty();
		editedProperty.bindBidirectional(new HtmlEditorListener(editor).editedProperty());

		attachmentPane = new FlowPane(Orientation.HORIZONTAL, 5, 0);
		attachmentPane.setPadding(new Insets(0, 10, 0, 10));

		attachments = new LinkedHashSet<File>();

		removeAttachPane.setStyle("-fx-background-color: grey");
		removeAttachPane.setOnDragExited(e -> setCenter(editor));
		removeAttachPane.setOnDragOver(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasContent(DND_REMOVE_FILE_DATA_FORMAT)) {
				e.acceptTransferModes(TransferMode.COPY);
			} else {
				e.consume();
			}
		});
		removeAttachPane.setOnDragDropped(e -> {
			final Dragboard db = e.getDragboard();
			final boolean remove = db.hasContent(DND_REMOVE_FILE_DATA_FORMAT);
			if (remove) {
				removeAttachment((File) db.getContent(DND_REMOVE_FILE_DATA_FORMAT));
			}
			e.setDropCompleted(remove);
			e.consume();
		});

		editorView = (WebView) editor.lookup(".web-view");
		editorView.setOnDragOver(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY);
			} else if (db.hasContent(DND_REMOVE_FILE_DATA_FORMAT)) {
				setCenter(removeAttachPane);
			} else {
				e.consume();
			}
		});
		editorView.setOnDragDropped(e -> {
			final Dragboard db = e.getDragboard();
			db.getFiles().forEach(f -> addAttachment(f));
			e.setDropCompleted(db.hasFiles());
			e.consume();
		});
		editorView.getEngine().locationProperty().addListener((ov, o, n) -> {
			if (!n.isEmpty()) {
				final String html = editor.getHtmlText();
				Platform.runLater(() -> {
					editorView.getEngine().getLoadWorker().cancel();
					editorView.getEngine().loadContent(html);
				});
				UrlHelper.open(n, p -> onMailtoCallback == null? null: onMailtoCallback.call(p));
			}
		});
	}

	@Override
	public void requestFocus() {
		editorView.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED
				, 100, 100, 200, 200, MouseButton.PRIMARY
				, 1, false, false, false, false, false, false, false, false, false, false, null));
		editor.requestFocus();
		editorView.fireEvent(new MouseEvent(MouseEvent.MOUSE_RELEASED
				, 100, 100, 200, 200, MouseButton.PRIMARY
				, 1, false, false, false, false, false, false, false, false, false, false, null));
	}

	public void setOnMailtoCallback(Callback<String, Void> callback) {
		this.onMailtoCallback = callback;
	}

	public BooleanProperty editedProperty() {
		return editedProperty;
	}

	public void setHtmlText(String html) {
		editor.setHtmlText(html);
	}

	public String getHtmlText() {
		return editor.getHtmlText();
	}

	public Set<File> getAttachments() {
		return attachments;
	}

	private void addAttachment(final File file) {
		setBottom(attachmentPane);
		if (!attachments.add(file)) {
			return;
		}
		final String name = file.getName();
		final HBox attachment = new HBox(3, new ImageView(ATTACHMENT), new Label(name));
		attachment.setPadding(new Insets(5));
		attachment.setCursor(Cursor.HAND);
		attachment.setOnMouseClicked(ev -> {
			try {
				Desktop.getDesktop().open(file);
			} catch (final IOException e) {
				LOGGER.error("starting {}", name, e);
			}
		});
		attachment.setOnDragDetected(e -> {
			final ClipboardContent content = new ClipboardContent();
			content.put(DND_REMOVE_FILE_DATA_FORMAT, file);
			attachment.startDragAndDrop(TransferMode.ANY).setContent(content);
		});
		attachmentPane.getChildren().add(attachment);
	}

	private void removeAttachment(File attachment) {
		int index = 0;
		for(final Iterator<File> i = attachments.iterator(); i.hasNext();) {
			final File file = i.next();
			if (attachment.equals(file)) {
				i.remove();
				attachmentPane.getChildren().remove(index);
				break;
			}
			index++;
		}
	}

	private boolean toolbarCleaned = false;
	private synchronized void cleanToolBar() {
		if (toolbarCleaned) {
			return;
		}
		toolbarCleaned = true;

		final HtmlEditorToolBarHelper helper = new HtmlEditorToolBarHelper(editor);
		helper.hideToolBar(Line.TOP);

		helper.removeItem(Line.BOTTOM, 0); //paragraph
		helper.removeItem(Line.BOTTOM, 2); //separator
		helper.removeItem(Line.BOTTOM, 6); //separator
		helper.removeItem(Line.BOTTOM, 6); //insert line

		helper.moveToolbarItem(Line.TOP, 15, Line.BOTTOM, 6); //bg color
		helper.moveToolbarItem(Line.TOP, 15, Line.BOTTOM, 7); //fg color
		helper.moveToolbarItem(Line.TOP, 12, Line.BOTTOM, 8); //bullet
		helper.moveToolbarItem(Line.TOP, 12, Line.BOTTOM, 9); //number
	}
}
