package net.anfoya.mail.browser.javafx.message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.AutoCompComboBoxListener;
import net.anfoya.mail.browser.mime.MimeMessageHelper;
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageComposer<M extends SimpleMessage, C extends SimpleContact> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageComposer.class);

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M, C> mailService;
	private final EventHandler<ActionEvent> updateHandler;
	private final MimeMessageHelper helper;

	private final BorderPane mainPane;
	private final GridPane headerPane;
	private final HBox toBox;

	private final HTMLEditor bodyEditor;
	private final ComboBox<String> fromCombo;
	private final ComboBox<String> toCombo;
	private final TextField ccField;
	private final TextField bccField;
	private final TextField subjectField;

	private M draft;

	public MessageComposer(final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M, C> mailService, final EventHandler<ActionEvent> updateHandler) {
		super(StageStyle.UNIFIED);
		setTitle("FisherMail / Agaar / Agamar / Agaram");
		getIcons().add(new Image(getClass().getResourceAsStream("Mail.png")));
		setScene(new Scene(new BorderPane(), 800, 600));

		this.mailService = mailService;
		this.updateHandler = updateHandler;

		helper = new MimeMessageHelper();
		mainPane = (BorderPane) getScene().getRoot();

		final ColumnConstraints widthConstraints = new ColumnConstraints(80);
		final ColumnConstraints growConstraints = new ColumnConstraints();
		growConstraints.setHgrow(Priority.ALWAYS);
		headerPane = new GridPane();
		headerPane.setVgap(5);
		headerPane.setHgap(5);
		headerPane.setPadding(new Insets(5));
		headerPane.prefWidthProperty().bind(widthProperty());
		headerPane.getColumnConstraints().add(widthConstraints);
		headerPane.getColumnConstraints().add(growConstraints);
		mainPane.setTop(headerPane);

		fromCombo = new ComboBox<String>();
		fromCombo.prefWidthProperty().bind(widthProperty());
		fromCombo.getItems().add("me");
		fromCombo.getSelectionModel().select(0);
		fromCombo.setDisable(true);

		toBox = new HBox(new Label("to"), new Label(" ..."));
		toBox.setAlignment(Pos.CENTER_LEFT);
		toBox.setOnMouseClicked(event -> {
			if (headerPane.getChildren().contains(fromCombo)) {
				toMiniHeader();
			} else {
				toFullHeader();
			}
		});

		final Map<String, C> emailContacts = new LinkedHashMap<String, C>();
		try {
			for(final C c: mailService.getContacts()) {
				emailContacts.put(c.getEmail(), c);
			}
		} catch (final MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		toCombo = new ComboBox<String>();
		toCombo.prefWidthProperty().bind(widthProperty());
		toCombo.setEditable(true);
		toCombo.getItems().setAll(emailContacts.keySet());
		toCombo.setCellFactory(listView -> {
			return new ListCell<String>() {
				@Override
			    public void updateItem(final String address, final boolean empty) {
			        super.updateItem(address, empty);
			        if (!empty) {
			        	setText(emailContacts.get(address).getFullname() + " <" + emailContacts.get(address).getEmail() + ">");
			        }
				}
			};
		});

		new AutoCompComboBoxListener(toCombo, address -> {
			return emailContacts.get(address).getEmail() + " " + emailContacts.get(address).getFullname();
		});

		ccField = new TextField();
		bccField = new TextField();
		subjectField = new TextField("FisherMail - test");

		toMiniHeader();

		bodyEditor = new HTMLEditor();
		mainPane.setCenter(bodyEditor);

		final Button discardButton = new Button("discard");
		discardButton.setOnAction(event -> {
			discard();
			close();
		});

		final Button saveButton = new Button("save");
		saveButton.setCancelButton(true);
		saveButton.setOnAction(event -> {
			save();
			close();
		});

		final Button sendButton = new Button("send");
		sendButton.setOnAction(event -> {
			send();
			close();
		});

		final HBox buttonBox = new HBox(5, discardButton, saveButton, sendButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(5));
		mainPane.setBottom(buttonBox);

		show();
	}

	public void newMessage() throws MailException {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				draft = mailService.createDraft(null);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("creting draft", event.getSource().getException()));
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
				reply(message, true);
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
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				draft = mailService.createDraft(message);
				final MimeMessage reply = (MimeMessage) message.getMimeMessage().reply(all);
				reply.setContent((Multipart) draft.getMimeMessage().getContent());
				reply.saveChanges();
				draft.setMimeDraft(reply);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("creting draft", event.getSource().getException()));
		task.setOnSucceeded(event -> {
			updateHandler.handle(null);
			initComposer(true);
		});
		ThreadPool.getInstance().submitHigh(task);
	}

	public void forward(final M message) {
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

		String to;
		try {
			to = InternetAddress.toString(draft.getMimeMessage().getRecipients(RecipientType.TO)).split(",")[0];
			to = MimeUtility.decodeText(to);
		} catch(final Exception e) {
			to = "";
		}
		toCombo.setValue(to);


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
	}

	private MimeMessage buildMessage() throws MessagingException {
		final MimeBodyPart bodyPart = new MimeBodyPart();
		bodyPart.setText(bodyEditor.getHtmlText(), StandardCharsets.UTF_8.name(), "html");

		final MimeMultipart multipart = new MimeMultipart();
		multipart.addBodyPart(bodyPart);

		InternetAddress to;
		try {
			to = new InternetAddress(toCombo.getValue().toString());
		} catch (final Exception e) {
			to = null;
			LOGGER.info("no recipient for draft");
		}

		final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
//		message.setFrom(new InternetAddress("frederic.antigny@gmail.com"));
		message.setSubject(subjectField.getText());
		message.setContent(multipart);
		if (to != null) {
			message.addRecipient(RecipientType.TO, to);
		}
		return message;
	}

	private void send() {
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
	}

	private void save() {
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
	}

	private void discard() {
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
	}

	private void toMiniHeader() {
		headerPane.getChildren().clear();
		headerPane.addRow(0, toBox, toCombo);
		headerPane.addRow(1, new Label("subject"), subjectField);
	}

	private void toFullHeader() {
		headerPane.getChildren().clear();
//		headerPane.addRow(0, new Label("from"), fromCombo);
		headerPane.addRow(0, new Label("to"), toCombo);
		headerPane.addRow(1, new Label("cc"), ccField);
		headerPane.addRow(2, new Label("bcc"), bccField);
		headerPane.addRow(3, new Label("subject"), subjectField);
	}
}
