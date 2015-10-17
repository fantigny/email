package net.anfoya.mail.composer.javafx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import net.anfoya.javafx.scene.control.HtmlEditorListener;

public class MailEditor extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailEditor.class);
    private static final Image ATTACHMENT = new Image(MailEditor.class.getResourceAsStream("/net/anfoya/mail/image/attachment.png"));

//	private static final String[] IMAGE_EXTENSIONS = { "jpg", "jpeg", "png", "gif" };

//	public class LoggerBridge {
//		public void info(String log) {
//			LOGGER.info(log);
//		}
//		public void debug(String log) {
//			LOGGER.info(log);
//		}
//	}

//	private static final String ENABLE_CARET_MOVE_JS =
//			"function mouseCoords(ev) {"
//			+ "		logger.debug('mouseCoords');"
//			+ "		if(ev.pageX || ev.pageY) {"
//			+ "			return {x:ev.pageX, y:ev.pageY};"
//			+ "		}"
//			+ "		return {"
//			+ "			x:ev.clientX + document.body.scrollLeft - document.body.clientLeft,"
//			+ "			y:ev.clientY + document.body.scrollTop  - document.body.clientTop"
//			+ "		};"
//			+ "}"
//			+ "logger.debug('JS mouseCoords done');"
//			+ "function mouseMove(ev){"
//			+ "		logger.debug('mouseMove');"
//			+ "		ev = ev || window.event;"
//			+ "		var mousePos = mouseCoords(ev);"
//			+ "}"
//			+ "logger.debug('JS mouseMove done');"
//			+ "document.onmousemove = mouseMove;"
//			+ "logger.debug('ENABLE_CARET_MOVE_JS done');";
//
//	private static final String ENABLE_CARET_MOVE_JS =
//			"document.onmousemove = mouseMove;"
//			+ "function mouseMove(ev) {"
//			+ "		logger.debug('mouseMove');"
//			+ "}"
//			+ "logger.debug('JS mouseMove done');"
//			+ "logger.debug('ENABLE_CARET_MOVE_JS done');";
//	private static final String DISABLE_CARET_MOVE_JS = "document.onmousemove = null;"
//			+ "logger.debug('DISABLE_CARET_MOVE_JS done');";

	private final HTMLEditor editor;
	private final WebView editorView;
	private final BooleanProperty editedProperty;

	private final FlowPane attachmentPane;
	private final Set<File> attachments;

	public MailEditor() {
		editor = new HTMLEditor();
		editor.setStyle("-fx-background-color: transparent; -fx-border-width: 0 0 1 0; -fx-border-color: lightgray; -fx-font-size: 11px;");
		setCenter(editor);

		editedProperty = new SimpleBooleanProperty();
		editedProperty.bindBidirectional(new HtmlEditorListener(editor).editedProperty());

		editorView = (WebView) editor.lookup(".web-view");
		editorView.setOnDragOver(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY);
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

		attachmentPane = new FlowPane(Orientation.HORIZONTAL, 5, 0);
		attachmentPane.setPadding(new Insets(0, 10, 0, 10));

		attachments = new LinkedHashSet<File>();

//		editorView.setOnDragEntered(e -> {
//			final Dragboard db = e.getDragboard();
//			if (db.hasContent(DataFormat.FILES)) {
//				for(final File f: db.getFiles()) {
//					final String filename = f.getName().toLowerCase();
//					for(final String ext: IMAGE_EXTENSIONS) {
//						if (filename.endsWith("." + ext)) {
//							activateCaretDrop(true);
//							break;
//						}
//					}
//				}
//			}
//		});
//		editorView.setOnDragExited(e -> activateCaretDrop(false));
//
//		editorEngine = editorView.getEngine();
//		editorEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
//			if (n == State.SUCCEEDED) {
//				((JSObject) editorEngine.executeScript("window")).setMember("logger", new LoggerBridge());
//				LOGGER.debug("JS logger bridge installed");
//			}
//		});
	}

//	private void activateCaretDrop(boolean activate) {
//		if (activate) {
//			LOGGER.info("activate");
//			editorEngine.executeScript(ENABLE_CARET_MOVE_JS);
//			final Document doc = editorEngine.getDocument();
//			final EventListener listener = new EventListener() {
//			    @Override
//				public void handleEvent(Event ev) {
//			        LOGGER.info("{}", ev);
//			    }
//			};
//			((EventTarget) doc.getDocumentElement()).addEventListener("click", listener, false);
//		} else {
//			editorEngine.executeScript(DISABLE_CARET_MOVE_JS);
//		}
//	}

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
		attachmentPane.getChildren().add(attachment);
	}
}
