package net.anfoya.mail.composer.javafx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

public class MessageComposer<M extends Message, C extends Contact> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageComposer.class);

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

	public MessageComposer(final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService, final EventHandler<ActionEvent> updateHandler) {
		super(StageStyle.UNIFIED);
		setTitle("FisherMail / Agaar / Agamar / Agaram");

		editedProperty = new SimpleBooleanProperty(false);

		final Image icon = new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png"));
		getIcons().add(icon);

		final Scene scene = new Scene(new BorderPane(), 800, 600);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/mail/css/Mail.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/combo_noarrow.css").toExternalForm());
		setScene(scene);

		this.mailService = mailService;
		this.updateHandler = updateHandler;

		// load contacts from server
		final Set<C> emailContacts = new LinkedHashSet<C>();
		try {
			emailContacts.addAll(mailService.getContacts());
		} catch (final MailException e) {
			LOGGER.error("loading contacts", e);
		}

		helper = new MessageHelper();
		mainPane = (BorderPane) getScene().getRoot();
		mainPane.setPadding(new Insets(3));

		toListBox = new RecipientListPane<C>("to: ", emailContacts);
		toListBox.setOnUpdateList(e -> editedProperty.set(true));
		HBox.setHgrow(toListBox, Priority.ALWAYS);

		ccListBox = new RecipientListPane<C>("cc/bcc:", emailContacts);
		ccListBox.setOnUpdateList(e -> editedProperty.set(true));

		bccListBox = new RecipientListPane<C>("bcc", emailContacts);
		bccListBox.setOnUpdateList(e -> editedProperty.set(true));

		subjectField = new TextField("FisherMail - test");
		subjectField.setStyle("-fx-background-color: transparent");
		subjectField.textProperty().addListener((ov, o, n) -> editedProperty.set(editedProperty.get() || !n.equals(o)));
		final HBox subjectBox = new HBox(3, new Label("subject:"), subjectField);
		subjectBox.setAlignment(Pos.CENTER_LEFT);
		subjectBox.getStyleClass().add("box-underline");
		HBox.setHgrow(subjectField, Priority.ALWAYS);

		headerBox = new VBox(3, toListBox, ccListBox, subjectBox);
		headerBox.setPadding(new Insets(3));
		mainPane.setTop(headerBox);

		ccListBox.textfocusedProperty().addListener((ov, o, n) -> showBcc());

		editor = new HTMLEditor();
		editor.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
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

		final HBox buttonBox = new HBox(5, discardButton, saveButton, sendButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(5));
		mainPane.setBottom(buttonBox);

		editedProperty.addListener((ov, o, n) -> {
			saveButton.setText(n? "save": "saved");
			editorListener.editedProperty().set(n);
		});
	}

	public void newMessage(final String recipient) throws MailException {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
				message.setContent("", "text/html");
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
		try {
			draft = mailService.getDraft(id);
		} catch (final MailException e) {
			LOGGER.error("loading draft for id {}", id, e);
		}
		if (draft != null) {
			initComposer(false);
		} else {
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
				final MimeMessage reply = (MimeMessage) message.getMimeMessage().reply(all);
				reply.setContent(message.getMimeMessage().getContent(), message.getMimeMessage().getContentType());
				reply.saveChanges();
				draft = mailService.createDraft(message);
				draft.setMimeDraft(reply);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("creating reply message", event.getSource().getException()));
		task.setOnSucceeded(e -> initComposer(false));
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
				toListBox.add(a);
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading recipients");
		}

		boolean displayCC = false;
		try {
			for(final String a: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.CC))) {
				ccListBox.add(a);
				displayCC = true;
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading cc list");
		}

		try {
			for(final String a: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.BCC))) {
				bccListBox.add(a);
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

		String html;
		try {
			html = helper.toHtml(message);
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
			// send focus to html editor
			final WebView view = (WebView) ((GridPane)((HTMLEditorSkin)editor.getSkin()).getChildren().get(0)).getChildren().get(2);
			view.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100, 200, 200, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null));
			editor.requestFocus();
			view.fireEvent(new MouseEvent(MouseEvent.MOUSE_RELEASED, 100, 100, 200, 200, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null));

			// start auto save
			autosave();
		});
	}

	private void autosave() {
		new Timer(true).schedule(new TimerTask() {
			@Override
			public void run() {
				if (editedProperty.get()) {
					save();
				}
			}
		}, 0, 60 * 1000);
	}

	private MimeMessage buildMessage() throws MessagingException {
		final MimeBodyPart bodyPart = new MimeBodyPart();
		bodyPart.setText(editor.getHtmlText(), StandardCharsets.UTF_8.name(), "html");

		final MimeMultipart multipart = new MimeMultipart();
		multipart.addBodyPart(bodyPart);

		final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
//		message.setFrom(new InternetAddress("frederic.antigny@gmail.com"));
		message.setSubject(subjectField.getText());
		message.setContent(multipart);

		message.addRecipients(RecipientType.TO, toListBox.getRecipients().toArray(new Address[0]));
		message.addRecipients(RecipientType.CC, ccListBox.getRecipients().toArray(new Address[0]));
		message.addRecipients(RecipientType.BCC, bccListBox.getRecipients().toArray(new Address[0]));

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
		final String cc = "cc:";
		if (!cc.equals(ccListBox.getTitle())) {
			ccListBox.setTitle(cc);
			headerBox.getChildren().add(2, bccListBox);
		}
	}
}
