package net.anfoya.mail.composer.javafx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.HTMLEditor;
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

public class MessageComposer<M extends Message, C extends Contact> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageComposer.class);

	private final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService;
	private final EventHandler<ActionEvent> updateHandler;
	private final MessageHelper helper;

	private final BorderPane mainPane;
	private final GridPane headerPane;
	private final HBox toLabelBox;

	private final HTMLEditor bodyEditor;
	private final TextField subjectField;

	private final RecipientListPane<C> toListPane;
	private final RecipientListPane<C> ccListPane;
	private final RecipientListPane<C> bccListPane;

	private final Button sendButton;

	private M draft;

	public MessageComposer(final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService, final EventHandler<ActionEvent> updateHandler) {
		super(StageStyle.UNIFIED);
		setTitle("FisherMail / Agaar / Agamar / Agaram");

		final Image icon = new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png"));
		getIcons().add(icon);

		final Scene scene = new Scene(new BorderPane(), 800, 600);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/mail/css/Mail.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/combo_noarrow.css").toExternalForm());
		setScene(scene);

		this.mailService = mailService;
		this.updateHandler = updateHandler;

		helper = new MessageHelper();
		mainPane = (BorderPane) getScene().getRoot();
		mainPane.setPadding(new Insets(3));
//		mainPane.setStyle("-fx-background-color: green");

		final ColumnConstraints widthConstraints = new ColumnConstraints(80);
		final ColumnConstraints growConstraints = new ColumnConstraints();
		growConstraints.setHgrow(Priority.ALWAYS);
		headerPane = new GridPane();
//		headerPane.setStyle("-fx-background-color: red");
		headerPane.setVgap(5);
		headerPane.setHgap(5);
		headerPane.setPadding(new Insets(5));
		headerPane.prefWidthProperty().bind(widthProperty());
		headerPane.getColumnConstraints().add(widthConstraints);
		headerPane.getColumnConstraints().add(growConstraints);
		mainPane.setTop(headerPane);

		toLabelBox = new HBox(new Label("to"), new Label(" ..."));
		toLabelBox.setAlignment(Pos.CENTER_LEFT);
		toLabelBox.setOnMouseClicked(event -> toFullHeader());

		// load contacts from server
		final Set<C> emailContacts = new LinkedHashSet<C>();
		try {
			emailContacts.addAll(mailService.getContacts());
		} catch (final MailException e) {
			LOGGER.error("loading contacts", e);
		}

		subjectField = new TextField("FisherMail - test");
		subjectField.setStyle("-fx-background-color: #cccccc");

		toListPane = new RecipientListPane<C>(emailContacts);
		ccListPane = new RecipientListPane<C>(emailContacts);
		bccListPane = new RecipientListPane<C>(emailContacts);

		toMiniHeader();

		bodyEditor = new HTMLEditor();
		bodyEditor.setPadding(new Insets(0, 0, 0, 5));
		mainPane.setCenter(bodyEditor);

		final Button discardButton = new Button("discard");
		discardButton.setOnAction(event -> discardAndClose());

		final Button saveButton = new Button("save");
		saveButton.setCancelButton(true);
		saveButton.setOnAction(event -> saveAndClose());

		sendButton = new Button("send");
		sendButton.setOnAction(e -> sendAndClose());

		final HBox buttonBox = new HBox(5, discardButton, saveButton, sendButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(5));
		mainPane.setBottom(buttonBox);

		show();
	}

	public void newMessage(final String recipient) throws MailException {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				draft = mailService.createDraft(null);
				toListPane.add(recipient);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("creating draft", event.getSource().getException()));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	public void editOrReply(final String id) {
		try {
			final M draft = mailService.getDraft(id);
			if (draft != null) {
				edit(draft);
			} else {
				final M message = mailService.getMessage(id);
				reply(message, false);
			}
		} catch (final MailException e) {
			LOGGER.error("loading draft or message", e);
		}
	}

	public void edit(final M draft) {
		this.draft = draft;
		initComposer(false);
	}

	public void reply(final M message, final boolean all) {
		sendButton.setText("reply");

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				draft = mailService.createDraft(message);
				final MimeMessage reply = (MimeMessage) message.getMimeMessage().reply(all);
				reply.setContent(draft.getMimeMessage().getContent(), draft.getMimeMessage().getContentType());
				reply.saveChanges();
				draft.setMimeDraft(reply);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("creating draft", event.getSource().getException()));
		task.setOnSucceeded(event -> {
			updateHandler.handle(null);
			initComposer(true);
		});
		ThreadPool.getInstance().submitHigh(task);
	}

	public void forward(final M message) {
		sendButton.setText("forward");

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				draft = mailService.createDraft(message);
				draft.setMimeDraft(message.getMimeMessage());
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("creting draft", event.getSource().getException()));
		task.setOnSucceeded(event -> {
			updateHandler.handle(null);
			initComposer(true);
			subjectField.setText("Fwd: " + subjectField.getText());
		});
		ThreadPool.getInstance().submitHigh(task);
	}

	private void initComposer(final boolean quote) {
		final MimeMessage message = draft.getMimeMessage();

		try {
			for(final String a: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.TO))) {
				toListPane.add(a);
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading recipients");
		}

		try {
			for(final String a: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.CC))) {
				ccListPane.add(a);
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading cc list");
		}

		try {
			for(final String a: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.BCC))) {
				bccListPane.add(a);
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading bcc list");
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
			LOGGER.error("getting body", e);
		}
		if (!html.isEmpty() && quote) {
			final StringBuffer sb = new StringBuffer("<br><br>");
			sb.append("<blockquote class='fsm_quote' style='margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex'>");
			sb.append(html);
			sb.append("</blockquote>");
			html = sb.toString();
		}
		bodyEditor.setHtmlText(html);

		if (quote) {
			bodyEditor.requestFocus();
		}
	}

	private MimeMessage buildMessage() throws MessagingException {
		final MimeBodyPart bodyPart = new MimeBodyPart();
		bodyPart.setText(bodyEditor.getHtmlText(), StandardCharsets.UTF_8.name(), "html");

		final MimeMultipart multipart = new MimeMultipart();
		multipart.addBodyPart(bodyPart);

		final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
//		message.setFrom(new InternetAddress("frederic.antigny@gmail.com"));
		message.setSubject(subjectField.getText());
		message.setContent(multipart);

		message.addRecipients(RecipientType.TO, toListPane.getRecipients().toArray(new Address[0]));
		message.addRecipients(RecipientType.CC, ccListPane.getRecipients().toArray(new Address[0]));
		message.addRecipients(RecipientType.BCC, bccListPane.getRecipients().toArray(new Address[0]));

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
		task.setOnFailed(event -> LOGGER.error("sending draft", event.getSource().getException()));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);

		close();
	}

	private void saveAndClose() {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
			    draft.setMimeDraft(buildMessage());
				mailService.save(draft);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("saving draft", event.getSource().getException()));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);

		close();
	}

	private void discardAndClose() {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.remove(draft);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("deleting draft", event.getSource().getException()));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);

		close();
	}

	private void toMiniHeader() {
		headerPane.getChildren().clear();
		headerPane.addRow(0, toLabelBox, toListPane);
		headerPane.addRow(1, new Label("subject"), subjectField);
	}

	private void toFullHeader() {
		headerPane.getChildren().clear();
//		headerPane.addRow(0, new Label("from"), fromCombo);
		headerPane.addRow(0, new Label("to"), toListPane);
		headerPane.addRow(1, new Label("cc"), ccListPane);
		headerPane.addRow(2, new Label("bcc"), bccListPane);
		headerPane.addRow(3, new Label("subject"), subjectField);
	}
}
