package net.anfoya.mail.composer.javafx;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.HtmlEditorListener;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.scene.web.skin.HTMLEditorSkin;

public class MailComposer<M extends Message, C extends Contact> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailComposer.class);

	private final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService;
	private final EventHandler<ActionEvent> updateHandler;
	private final MessageHelper helper;

	private final BorderPane mainPane;

	private final VBox headerBox;
	private final TextField subjectField;

	private final HTMLEditor editor;
	private final HtmlEditorListener editorListener;

	private final RecipientListPane<C> toListBox;
	private final RecipientListPane<C> ccListBox;
	private final RecipientListPane<C> bccListBox;

	private final Button saveButton;
	private final Button sendButton;

	private M draft;

	private final BooleanProperty editedProperty;
	private final Timer autosaveTimer;

	private final Map<String, C> addressContacts;

	public MailComposer(final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService, final EventHandler<ActionEvent> updateHandler) {
		super(StageStyle.UNIFIED);
		setOnCloseRequest(e -> stopAutosave());

		setTitle("FisherMail");

		editedProperty = new SimpleBooleanProperty(false);
		autosaveTimer = new Timer(true);

		final Image icon = new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png"));
		getIcons().add(icon);

		final Scene scene = new Scene(new BorderPane(), 800, 600);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/mail/css/Mail.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/combo_noarrow.css").toExternalForm());
		setScene(scene);

		this.mailService = mailService;
		this.updateHandler = updateHandler;

		// load contacts from server
		addressContacts = new ConcurrentHashMap<String, C>();
		initContacts();

		helper = new MessageHelper();
		mainPane = (BorderPane) getScene().getRoot();
		mainPane.setPadding(new Insets(3));

		toListBox = new RecipientListPane<C>("to: ");
		toListBox.setOnUpdateList(e -> editedProperty.set(true));
		HBox.setHgrow(toListBox, Priority.ALWAYS);

		ccListBox = new RecipientListPane<C>("cc/bcc: ");
		ccListBox.setFocusTraversable(false);
		ccListBox.setOnUpdateList(e -> editedProperty.set(true));

		bccListBox = new RecipientListPane<C>("bcc: ");
		bccListBox.setOnUpdateList(e -> editedProperty.set(true));

		final Label subject = new Label("subject:");
		subject.setStyle("-fx-text-fill: gray");
		subjectField = new TextField("FisherMail - test");
		subjectField.setStyle("-fx-background-color: transparent");
		subjectField.textProperty().addListener((ov, o, n) -> editedProperty.set(editedProperty.get() || !n.equals(o)));
		final HBox subjectBox = new HBox(0, subject, subjectField);
		subjectBox.setAlignment(Pos.CENTER_LEFT);
		subjectBox.getStyleClass().add("box-underline");
		HBox.setHgrow(subjectField, Priority.ALWAYS);

		headerBox = new VBox(0, toListBox, ccListBox, subjectBox);
		headerBox.setPadding(new Insets(3, 10, 5, 10));
		mainPane.setTop(headerBox);

		ccListBox.focusedProperty().addListener((ov, o, n) -> showBcc());

		editor = new HTMLEditor();
		editor.setStyle("-fx-background-color: transparent; -fx-border-width: 0 0 1 0; -fx-border-color: lightgray; -fx-font-size: 11px;");
		editor.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            } else {
                e.consume();
            }
        });
		editor.setOnDragDropped(e -> {
            final Dragboard dragboard = e.getDragboard();
            if (dragboard.hasFiles()) {
	            for (final File file: dragboard.getFiles()) {
	            	addAttachment(file);
	            }
            }
            e.setDropCompleted(dragboard.hasFiles());
            e.consume();
        });
		mainPane.setCenter(editor);

		editorListener = new HtmlEditorListener(editor);
		editorListener.editedProperty().addListener((ov, o, n) -> editedProperty.set(editedProperty.get() || n));

		final Button discardButton = new Button("discard");
		discardButton.setOnAction(event -> discardAndClose());

		saveButton = new Button("save");
		saveButton.setOnAction(event -> save());
		saveButton.disableProperty().bind(editedProperty.not());

		sendButton = new Button("send");
		sendButton.setOnAction(e -> sendAndClose());

		final HBox buttonBox = new HBox(10, discardButton, saveButton, sendButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(8, 5, 5, 5));
		mainPane.setBottom(buttonBox);

		editedProperty.addListener((ov, o, n) -> {
			saveButton.setText(n? "save": "saved");
			editorListener.editedProperty().set(n);
		});
	}

	private void initContacts() {
		final Task<Set<C>> contactTask = new Task<Set<C>>() {
			@Override
			protected Set<C> call() throws Exception {
				return mailService.getContacts();
			}
		};
		contactTask.setOnSucceeded(e -> {
			for(final C c: contactTask.getValue()) {
				addressContacts.put(c.getEmail(), c);
			}
			toListBox.setAddressContacts(addressContacts);
			ccListBox.setAddressContacts(addressContacts);
			bccListBox.setAddressContacts(addressContacts);

			final C contact = mailService.getContact();
			if (contact.getFullname().isEmpty()) {
				setTitle(getTitle() + " - " + contact.getEmail());
			} else {
				setTitle(getTitle() + " - " + contact.getFullname() + " (" + contact.getEmail() + ")");
			}
		});
		contactTask.setOnFailed(e -> LOGGER.error("loading contacts", e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(contactTask);
	}

	private void addAttachment(final File file) {

	}

	public void newMessage(final String recipient) throws MailException {
		final InternetAddress to;
		if (recipient == null || recipient.isEmpty()) {
			to = null;
		} else {
			InternetAddress address;
			try {
				address = new InternetAddress(recipient);
			} catch (final AddressException e) {
				address = null;
			}
			to = address;
		}

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
				message.setContent("", "text/html");
				if (to != null) {
					message.addRecipient(RecipientType.TO, to);
				}
				message.saveChanges();
				draft = mailService.createDraft(null);
				draft.setMimeDraft(message);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("creating draft", e.getSource().getException()));
		task.setOnSucceeded(e -> initComposer(false));
		ThreadPool.getInstance().submitHigh(task);
	}

	public void editOrReply(final String id) {
		// try to find a draft with this id
		try {
			draft = mailService.getDraft(id);
		} catch (final MailException e) {
			LOGGER.error("loading draft for id {}", id, e);
		}
		if (draft != null) {
			// edit
			initComposer(false);
		} else {
			// reply
			try {
				final M message = mailService.getMessage(id);
				reply(message, false);
			} catch (final MailException e) {
				LOGGER.error("loading message for id {}", id, e);
			}
		}
	}

	public void reply(final M message, final boolean all) {
		sendButton.setText("reply");

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				LOGGER.warn(message.getMimeMessage().getReplyTo()[0].toString());
				final MimeMessage reply = (MimeMessage) message.getMimeMessage().reply(all);
				reply.setContent(message.getMimeMessage().getContent(), message.getMimeMessage().getContentType());
				reply.saveChanges();
				draft = mailService.createDraft(message);
				draft.setMimeDraft(reply);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("creating reply message", event.getSource().getException()));
		task.setOnSucceeded(e -> initComposer(true));
		ThreadPool.getInstance().submitHigh(task);
	}

	public void forward(final M message) {
		sendButton.setText("forward");

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				final MimeMessage forward = new MimeMessage(Session.getDefaultInstance(new Properties()));
				forward.setSubject("Fwd: " + message.getMimeMessage().getSubject());
				forward.setContent(message.getMimeMessage().getContent(), message.getMimeMessage().getContentType());
				forward.saveChanges();
				draft = mailService.createDraft(message);
				draft.setMimeDraft(forward);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("creating forward message", event.getSource().getException()));
		task.setOnSucceeded(e -> initComposer(true));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void initComposer(final boolean quote) {
		final MimeMessage message = draft.getMimeMessage();
		updateHandler.handle(null);

		try {
			for(final String a: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.TO))) {
				toListBox.addRecipient(a);
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading recipients");
		}

		boolean displayCC = false;
		try {
			for(final String a: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.CC))) {
				ccListBox.addRecipient(a);
				displayCC = true;
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading cc list");
		}

		try {
			for(final String a: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.BCC))) {
				bccListBox.addRecipient(a);
				displayCC = true;
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading bcc list");
		}

		if (displayCC) {
			showBcc();
		}

		String subject;
		try {
			subject = MimeUtility.decodeText(message.getSubject());
		} catch (final Exception e) {
			subject = "";
		}
		subjectField.setText(subject);

		String html = "<style>"
				+ "html {"
				+ " line-height: 1em !important;"
				+ " font-size: 14px !important;"
				+ " font-family: Lucida Grande !important;"
				+ " color: #222222 !important;"
				+ " background-color: #FDFDFD !important;}"
				+ "p {"
				+ " padding: 0 !important;"
				+ " margin: 2px 0 !important; }"
				+ "</style>";
		try {
			html += helper.toHtml(message);

		} catch (IOException | MessagingException e) {
			html = "";
			LOGGER.error("getting html content", e);
		}
		if (!html.isEmpty() && quote) {
			final StringBuffer sb = new StringBuffer("<br><br>");
			sb.append("<blockquote class='fsm_quote' style='margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex'>");
			sb.append(html);
			sb.append("</blockquote>");
			html = sb.toString();
		}
		editor.setHtmlText(html);

		show();

		Platform.runLater(() -> {
			if (quote) {
				// send focus to html editor
				final WebView view = (WebView) ((GridPane)((HTMLEditorSkin)editor.getSkin()).getChildren().get(0)).getChildren().get(2);
				view.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100, 200, 200, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null));
				editor.requestFocus();
				view.fireEvent(new MouseEvent(MouseEvent.MOUSE_RELEASED, 100, 100, 200, 200, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null));
			} else {
				toListBox.requestFocus();
			}

			// start auto save
			startAutosave();
		});
	}

	private void startAutosave() {
		autosaveTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (editedProperty.get()) {
					save();
				}
			}
		}, 0, 60 * 1000);
	}

	private void stopAutosave() {
		autosaveTimer.cancel();
	}

	private MimeMessage buildMessage() throws MessagingException {
		final MimeBodyPart bodyPart = new MimeBodyPart();
		bodyPart.setText(editor.getHtmlText(), StandardCharsets.UTF_8.name(), "html");

		final MimeMultipart multipart = new MimeMultipart();
		multipart.addBodyPart(bodyPart);

		final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
		message.setSubject(subjectField.getText());
		message.setContent(multipart);

		Address from;
		C contact;
		try {
			contact = mailService.getContact();
			if (contact.getFullname().isEmpty()) {
				from = new InternetAddress(contact.getEmail());
			} else {
				from = new InternetAddress(contact.getEmail(), contact.getFullname());
			}
			message.setFrom(from);
		} catch (final UnsupportedEncodingException e) {
			LOGGER.error("loading user data", e);
		}

		for(final String address: toListBox.getRecipients()) {
			if (addressContacts.containsKey(address)) {
				try {
					message.addRecipient(RecipientType.TO, new InternetAddress(address, addressContacts.get(address).getFullname()));
				} catch (final UnsupportedEncodingException e) {
					message.addRecipient(RecipientType.TO, new InternetAddress(address));
				}
			} else {
				message.addRecipient(RecipientType.TO, new InternetAddress(address));
			}
		}
		for(final String address: ccListBox.getRecipients()) {
			if (addressContacts.containsKey(address)) {
				try {
					message.addRecipient(RecipientType.CC, new InternetAddress(address, addressContacts.get(address).getFullname()));
				} catch (final UnsupportedEncodingException e) {
					message.addRecipient(RecipientType.CC, new InternetAddress(address));
				}
			} else {
				message.addRecipient(RecipientType.CC, new InternetAddress(address));
			}
		}
		for(final String address: bccListBox.getRecipients()) {
			if (addressContacts.containsKey(address)) {
				try {
					message.addRecipient(RecipientType.BCC, new InternetAddress(address, addressContacts.get(address).getFullname()));
				} catch (final UnsupportedEncodingException e) {
					message.addRecipient(RecipientType.BCC, new InternetAddress(address));
				}
			} else {
				message.addRecipient(RecipientType.BCC, new InternetAddress(address));
			}
		}

		return message;
	}

	private void sendAndClose() {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
			    draft.setMimeDraft(buildMessage());
				mailService.send(draft);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("sending draft", e.getSource().getException()));
		task.setOnSucceeded(e -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);

		close();
	}

	private void save() {
		Platform.runLater(() -> {
			editedProperty.set(false);
			saveButton.setText("saving");
		});

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
			    draft.setMimeDraft(buildMessage());
				mailService.save(draft);
				return null;
			}
		};
		task.setOnFailed(e -> {
			editedProperty.set(true);
			LOGGER.error("saving draft", e.getSource().getException());
		});
		task.setOnSucceeded(e -> {
			saveButton.setText("saved");
		});
		ThreadPool.getInstance().submitHigh(task);
	}

	private void discardAndClose() {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.remove(draft);
				return null;
			}
		};
		task.setOnFailed(e -> LOGGER.error("deleting draft", e.getSource().getException()));
		task.setOnSucceeded(e -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);

		close();
	}

	private void showBcc() {
		final String cc = "cc: ";
		if (!cc.equals(ccListBox.getTitle())) {
			ccListBox.setTitle(cc);
			ccListBox.setFocusTraversable(true);
			headerBox.getChildren().add(2, bccListBox);
		}
	}
}
