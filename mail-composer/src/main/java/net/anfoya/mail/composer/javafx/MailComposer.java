package net.anfoya.mail.composer.javafx;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.util.VoidCallback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.browser.javafx.css.CssHelper;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.mime.MessageReader;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;

public class MailComposer<M extends Message, C extends Contact> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailComposer.class);
	private static final int AUTO_SAVE_DELAY = 60; // seconds

	private final MailService<?, ?, ?, M, C> mailService;
	private final Settings settings;

	private final String myAddress;

	private final BorderPane mainPane;

	private final VBox headerBox;
	private final TextField subjectField;

	private final RecipientListPane<C> toListBox;
	private final RecipientListPane<C> ccListBox;
	private final RecipientListPane<C> bccListBox;

	private final MailEditor editor;
	private final BooleanProperty editedProperty;
	private Timer autosaveTimer;

	private final Map<String, C> addressContacts;

	private final Button saveButton;

	private M draft;
	private M source;
	
	private VoidCallback<M> sendCallback;
	private VoidCallback<M> discardCallback;
	private VoidCallback<String> openUrlCallback;

	public MailComposer(final MailService<?, ?, ?, M, C> mailService, final Settings settings) {
		super(StageStyle.UNIFIED);
		setTitle("FisherMail");

		myAddress = mailService.getContact().getEmail();
		editedProperty = new SimpleBooleanProperty(false);
		autosaveTimer = null;
		sendCallback = discardCallback = m -> {};


		final Image icon = new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/Mail.png"));
		getIcons().add(icon);

		final Scene scene = new Scene(new BorderPane(), 800, 600, Color.TRANSPARENT);
		CssHelper.addCommonCss(scene);
		CssHelper.addCss(scene, "/net/anfoya/javafx/scene/control/combo_noarrow.css");
		setScene(scene);

		this.mailService = mailService;
		this.settings = settings;

		// load contacts from server
		addressContacts = new ConcurrentHashMap<String, C>();
		initContacts();

		mainPane = (BorderPane) getScene().getRoot();
		mainPane.setPadding(new Insets(3));

		toListBox = new RecipientListPane<C>("to: ");
		toListBox.setOnUpdateList(e -> editedProperty.set(true));
		HBox.setHgrow(toListBox, Priority.ALWAYS);

		ccListBox = new RecipientListPane<C>("cc/bcc: ");
		ccListBox.setFocusTraversable(false);
		ccListBox.setOnUpdateList(e -> editedProperty.set(true));
		HBox.setHgrow(ccListBox, Priority.ALWAYS);

		bccListBox = new RecipientListPane<C>("bcc: ");
		bccListBox.setOnUpdateList(e -> editedProperty.set(true));
		HBox.setHgrow(bccListBox, Priority.ALWAYS);

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
		headerBox.setPadding(new Insets(3, 10, 0, 10));
		mainPane.setTop(headerBox);

		ccListBox.focusedProperty().addListener((ov, o, n) -> showBcc());

		editor = new MailEditor();
		editor.editedProperty().addListener((ov, o, n) -> editedProperty.set(editedProperty.get() || n));
		editor.setOnMailtoCallback(p -> {
			try {
				final MailComposer<M, C> composer = new MailComposer<M, C>(mailService, settings);
				composer.setOnSend(sendCallback);
				composer.setOnDiscard(discardCallback);
				composer.setOnOpenUrl(openUrlCallback);
				composer.newMessage(p);
			} catch (final MailException e) {
				LOGGER.error("create new mail to {}", p, e);
			}
			return null;
		});

		mainPane.setCenter(editor);

		final Button discardButton = new Button("discard");
		discardButton.setOnAction(event -> discardAndClose());

		saveButton = new Button("save");
		saveButton.setOnAction(event -> save());
		saveButton.disableProperty().bind(editedProperty.not());

		final Button sendButton = new Button("send");
		sendButton.setOnAction(e -> sendAndClose());

		final HBox buttonBox = new HBox(10, discardButton, saveButton, sendButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(8, 5, 5, 5));
		mainPane.setBottom(buttonBox);

		editedProperty.addListener((ov, o, n) -> {
			saveButton.setText(n? "save": "saved");
			editor.editedProperty().set(n);
			if (n) {
				startAutosave();
			} else {
				stopAutosave();
			}
		});
	}

	public void setOnOpenUrl(VoidCallback<String> callback) {
		openUrlCallback = callback;
	}

	public void setOnDiscard(VoidCallback<M> callback) {
		discardCallback = callback;
	}

	public void setOnSend(VoidCallback<M> callback) {
		sendCallback = callback;
	}

	@Override
	public void close() {
		stopAutosave();
		super.close();
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
//				setTitle("FisherMail - Fred A. (abc.xyz@gmail.com)");
			}
		});
		contactTask.setOnFailed(e -> LOGGER.error("load contacts", e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "load contacts", contactTask);
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
		task.setOnFailed(e -> LOGGER.error("create  draft", e.getSource().getException()));
		task.setOnSucceeded(e -> initComposer(false, true));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "create  draft", task);
	}

	public void editOrReply(final String id, final boolean all) {
		// try to find a draft with this id
		try {
			draft = mailService.getDraft(id);
		} catch (final MailException e) {
			LOGGER.error("load draft for id {}", id, e);
		}
		if (draft != null) {
			// edit
			initComposer(false, false);
		} else {
			// reply
			try {
				final M message = mailService.getMessage(id);
				reply(message, all);
			} catch (final MailException e) {
				LOGGER.error("load message for id {}", id, e);
			}
		}
	}

	public void reply(final M source, final boolean all) {
		this.source = source;
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				final MimeMessage reply = (MimeMessage) source.getMimeMessage().reply(all);
				reply.setContent(source.getMimeMessage().getContent(), source.getMimeMessage().getContentType());
				reply.saveChanges();
				MessageHelper.removeMyselfFromRecipient(myAddress, reply);
				draft = mailService.createDraft(source);
				draft.setMimeDraft(reply);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("create  reply draft", event.getSource().getException()));
		task.setOnSucceeded(e -> initComposer(true, true));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "create  reply draft", task);
	}

	public void forward(final M source) {
		this.source = source;
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				final MimeMessage forward = new MimeMessage(Session.getDefaultInstance(new Properties()));
				forward.setSubject("Fwd: " + source.getMimeMessage().getSubject());
				forward.setContent(source.getMimeMessage().getContent(), source.getMimeMessage().getContentType());
				forward.saveChanges();
				draft = mailService.createDraft(source);
				draft.setMimeDraft(forward);
				return null;
			}
		};
		task.setOnFailed(event -> LOGGER.error("create  forward draft", event.getSource().getException()));
		task.setOnSucceeded(e -> initComposer(true, true));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "create  forward draft", task);
	}

	private void initComposer(final boolean quote, final boolean signature) {
		final MimeMessage message = draft.getMimeMessage();

		try {
			for(final String a: MessageHelper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.TO))) {
				toListBox.addRecipient(a);
			}
		} catch (final MessagingException e) {
			LOGGER.error("read recipients");
		}

		boolean displayCC = false;
		try {
			for(final String a: MessageHelper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.CC))) {
				ccListBox.addRecipient(a);
				displayCC = true;
			}
		} catch (final MessagingException e) {
			LOGGER.error("read cc list");
		}

		try {
			for(final String a: MessageHelper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.BCC))) {
				bccListBox.addRecipient(a);
				displayCC = true;
			}
		} catch (final MessagingException e) {
			LOGGER.error("read bcc list");
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
			html = new MessageReader().toHtml(message);
		} catch (IOException | MessagingException e) {
			html = "";
			LOGGER.error("get html content", e);
		}
		if (quote && !html.isEmpty()) {
			try {
				final MimeMessage srcMess = source.getMimeMessage();
				String sender = "[empty]";
				if (srcMess.getFrom() != null && srcMess.getFrom().length > 0) {
					sender = MessageHelper.getName((InternetAddress) srcMess.getFrom()[0]);
				}
				html = MessageHelper.quote(srcMess.getSentDate(), sender, html);
			} catch (final MessagingException e) {
				LOGGER.error("quote older content", e);
			}
		}
		if (signature) {
			html = MessageHelper.addSignature(html, settings.htmlSignature().get());
		}
		html = MessageHelper.addStyle(html);
		editor.setHtmlText(html);

		show();

		Platform.runLater(() -> {
			if (quote) {
				editor.requestFocus();
			} else {
				toListBox.requestFocus();
			}
		});
	}

	private synchronized void startAutosave() {
		if (autosaveTimer == null) {
			LOGGER.info("start auto save ({}s)", AUTO_SAVE_DELAY);
			autosaveTimer = new Timer("autosave-draft-timer", true);
			autosaveTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					save();
				}
			}, AUTO_SAVE_DELAY * 1000);
		}
	}

	private synchronized void stopAutosave() {
		if (autosaveTimer != null) {
			LOGGER.info("stop auto save");
			autosaveTimer.cancel();
			autosaveTimer = null;
		}
	}

	private void save() {
		stopAutosave();
		LOGGER.info("save draft");
		Platform.runLater(() -> saveButton.setText("saving"));

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
			LOGGER.error("save  draft", e.getSource().getException());
		});
		task.setOnSucceeded(e -> {
			editedProperty.set(false);
			saveButton.setText("saved");
		});
		ThreadPool.getDefault().submit(PoolPriority.MAX, "save  draft", task);
	}

	private MimeMessage buildMessage() throws MessagingException {
		final String html = editor.getHtmlText();
		LOGGER.debug(html);

		final MimeBodyPart bodyPart = new MimeBodyPart();
		bodyPart.setText(html, StandardCharsets.UTF_8.name(), "html");

		final MimeMultipart multipart = new MimeMultipart();
		multipart.addBodyPart(bodyPart);

		editor.getAttachments().forEach(f -> {
			LOGGER.debug("add attachment {}", f);
			try {
				final MimeBodyPart part = new MimeBodyPart();
				part.attachFile(f);
				part.setFileName(MimeUtility.encodeText(f.getName()));
				multipart.addBodyPart(part);
			} catch (final Exception e) {
				LOGGER.error("add attachment {}", f);
			}
		});

		final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
		message.setSubject(subjectField.getText());
		message.setContent(multipart);

		Address from;
		try {
			final C contact = mailService.getContact();
			if (contact.getFullname().isEmpty()) {
				from = new InternetAddress(contact.getEmail());
			} else {
				from = new InternetAddress(contact.getEmail(), contact.getFullname());
			}
			message.setFrom(from);
		} catch (final UnsupportedEncodingException e) {
			LOGGER.error("load user data", e);
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
		try {
			draft.setMimeDraft(buildMessage());
		} catch (MessagingException e) {
			//TODO display error message
			LOGGER.error("build message", e);
		}
		
	    sendCallback.call(draft);
		close();
	}

	private void discardAndClose() {
	    discardCallback.call(draft);
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
