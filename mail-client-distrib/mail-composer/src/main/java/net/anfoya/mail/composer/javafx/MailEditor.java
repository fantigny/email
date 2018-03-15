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
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
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
import net.anfoya.java.util.VoidCallback;
import net.anfoya.javafx.scene.control.HtmlEditorListener;
import net.anfoya.javafx.scene.control.HtmlEditorToolBarHelper;
import net.anfoya.javafx.scene.control.HtmlEditorToolBarHelper.Line;
import net.anfoya.mail.browser.javafx.util.UrlHelper;

public class MailEditor extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailEditor.class);
    private static final Image ATTACHMENT = new Image(MailEditor.class.getResourceAsStream("/net/anfoya/mail/img/attachment.png"));

	private final HTMLEditor editor;
	private final WebView editorView;
	private final BooleanProperty editedProperty;

	private final FlowPane attachmentPane;
	private final Set<File> attachments;

	private VoidCallback<String> composeCallback;

	public MailEditor() {
		final StackPane stackPane = new StackPane();
		stackPane.setAlignment(Pos.BOTTOM_CENTER);
		setCenter(stackPane);

		editor = new HTMLEditor();
		editor.getStyleClass().add("message-editor");
		stackPane.getChildren().add(editor);
		cleanEditorToolBar();

		editedProperty = new SimpleBooleanProperty();
		editedProperty.bindBidirectional(new HtmlEditorListener(editor).editedProperty());

		attachmentPane = new FlowPane(Orientation.HORIZONTAL, 5, 0);
		attachmentPane.setPadding(new Insets(0, 10, 0, 10));

		attachments = new LinkedHashSet<>();

		final AttchDropPane attachDropPane = new AttchDropPane();
		attachDropPane.prefWidthProperty().bind(stackPane.widthProperty());
		attachDropPane.setOnRemove(f -> removeAttachment(f));

		stackPane.setOnDragExited(e -> {
			if (stackPane.getChildren().contains(attachDropPane)) {
				stackPane.getChildren().remove(attachDropPane);
			}
		});

		editorView = (WebView) editor.lookup(".web-view");
		editorView.setOnDragOver(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY);
			} else if (db.hasContent(AttchDropPane.FILE_DATA_FORMAT)
					&& !stackPane.getChildren().contains(attachDropPane)) {
				stackPane.getChildren().add(attachDropPane);
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
				UrlHelper.open(n, r -> {
					if (composeCallback != null) {
						composeCallback.call(r);
					}
				});
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

	public void setOnCompose(VoidCallback<String> callback) {
		this.composeCallback = callback;
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
				LOGGER.error("start {}", name, e);
			}
		});
		attachment.setOnDragDetected(e -> {
			final ClipboardContent content = new ClipboardContent();
			content.put(AttchDropPane.FILE_DATA_FORMAT, file);
			attachment.startDragAndDrop(TransferMode.ANY).setContent(content);
		});
		attachmentPane.getChildren().add(attachment);
	}

	private Void removeAttachment(File attachment) {
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
		return null;
	}

	private void cleanEditorToolBar() {
		LOGGER.debug("clean tool bars");

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

		helper.doWhenReady();
	}
}
