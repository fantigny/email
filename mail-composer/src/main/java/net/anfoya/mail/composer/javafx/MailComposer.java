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
import net.anfoya.mail.client.App;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.mime.MessageReader;
import net.anfoya.mail.model.Contact;
import net.anfoya.mail.model.Message;
import net.anfoya.mail.service.MailService;

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
	private VoidCallback<String> composeCallback;

	public MailComposer(final MailService<?, ?, ?, M, C> mailService, final Settings settings) {
		super(StageStyle.UNIFIED);
		setTitle(App.getName());
		getIcons().add(App.getIcon());

		this.mailService = mailService;
		this.settings = settings;

		myAddress = mailService.getContact().getEmail();
		editedProperty = new SimpleBooleanProperty(false);
		autosaveTimer = null;
		sendCallback = discardCallback = m -> {
		};

		final Scene scene = new Scene(new BorderPane(), 800, 600, Color.TRANSPARENT);
		CssHelper.addCommonCss(scene);
		CssHelper.addCss(scene, "/net/anfoya/javafx/scene/control/combo_noarrow.css");
		setScene(scene);

		mainPane = (BorderPane) getScene().getRoot();
		mainPane.setPadding(new Insets(3));

		toListBox = new RecipientListPane<>("to: ");
		toListBox.setOnUpdateList(e -> editedProperty.set(true));
		HBox.setHgrow(toListBox, Priority.ALWAYS);

		ccListBox = new RecipientListPane<>("cc/bcc: ");
		ccListBox.setFocusTraversable(false);
		ccListBox.setOnUpdateList(e -> editedProperty.set(true));
		HBox.setHgrow(ccListBox, Priority.ALWAYS);

		bccListBox = new RecipientListPane<>("bcc: ");
		bccListBox.setOnUpdateList(e -> editedProperty.set(true));
		HBox.setHgrow(bccListBox, Priority.ALWAYS);

		final Label subject = new Label("subject:");
		subject.setStyle("-fx-text-fill: gray");
		subjectField = new TextField(App.getName() + " - test");
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
		editor.setOnCompose(r -> composeCallback.call(r));

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

		// load contacts from server
		addressContacts = new ConcurrentHashMap<>();
		initContacts();

		// listen for user input
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
			for (final C c : contactTask.getValue()) {
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
		ThreadPool.getDefault().submit(PoolPriority.MAX, "load contacts", contactTask);
	}

	public void compose(M draft, String recipient) {
		this.draft = draft;

		InternetAddress to;
		try {
			to = new InternetAddress(recipient);
		} catch (final AddressException e) {
			to = null;
		}

		final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
		try {
			message.setContent("", "text/html");
			if (to != null) {
				message.addRecipient(RecipientType.TO, to);
			}
			message.saveChanges();
		} catch (final MessagingException e) {
			LOGGER.error("composing", e);
		}
		draft.setMimeDraft(message);
		initComposer(false, true);
	}

	public void edit(final M draft) {
		this.draft = draft;
		initComposer(false, false);
	}

	public void reply(final M draft, final M source, final boolean all) {
		this.draft = draft;
		this.source = source;

		MimeMessage reply;
		try {
			reply = (MimeMessage) source.getMimeMessage().reply(all);
			reply.setContent(source.getMimeMessage().getContent(), source.getMimeMessage().getContentType());
			reply.saveChanges();
			MessageHelper.removeMyselfFromRecipient(myAddress, reply);
			draft.setMimeDraft(reply);
		} catch (final IOException | MessagingException e) {
			LOGGER.error("replying", e);
		}

		initComposer(true, true);
	}

	public void forward(final M draft, M source) {
		this.draft = draft;
		this.source = source;

		final MimeMessage forward = new MimeMessage(Session.getDefaultInstance(new Properties()));
		try {
			forward.setSubject("Fwd: " + source.getMimeMessage().getSubject());
			forward.setContent(source.getMimeMessage().getContent(), source.getMimeMessage().getContentType());
			forward.saveChanges();
			draft.setMimeDraft(forward);
		} catch (final IOException | MessagingException e) {
			LOGGER.error("forwarding", e);
		}

		initComposer(true, true);
	}

	private void initComposer(final boolean quote, final boolean signature) {
		final MimeMessage message = draft.getMimeMessage();

		try {
			for (final String a : MessageHelper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.TO))) {
				toListBox.addRecipient(a);
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading recipients");
		}

		boolean displayCC = false;
		try {
			for (final String a : MessageHelper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.CC))) {
				ccListBox.addRecipient(a);
				displayCC = true;
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading cc list");
		}

		try {
			for (final String a : MessageHelper
					.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.BCC))) {
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

		String html;
		try {
			html = new MessageReader().toHtml(message);
		} catch (IOException | MessagingException e) {
			html = "";
			LOGGER.error("getting html content", e);
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
				LOGGER.error("quoting original content", e);
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
		if (autosaveTimer != null) {
			// already started
			return;
		}

		autosaveTimer = new Timer("autosave-draft-timer", true);
		autosaveTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				save();
			}
		}, AUTO_SAVE_DELAY * 1000);

		LOGGER.info("auto save started ({}s)", AUTO_SAVE_DELAY);
	}

	private synchronized void stopAutosave() {
		if (autosaveTimer == null) {
			// not started
			return;
		}

		autosaveTimer.cancel();
		autosaveTimer = null;

		LOGGER.info("auto save stopped");
	}

	private void save() {
		stopAutosave();

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
			LOGGER.error("saving draft", e.getSource().getException());
		});
		task.setOnSucceeded(e -> {
			editedProperty.set(false);
			saveButton.setText("saved");
			LOGGER.info("draft saved");
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
				LOGGER.error("adding attachment {}", f);
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
			LOGGER.error("loading user data", e);
		}

		for (final String address : toListBox.getRecipients()) {
			if (addressContacts.containsKey(address)) {
				try {
					message.addRecipient(RecipientType.TO,
							new InternetAddress(address, addressContacts.get(address).getFullname()));
				} catch (final UnsupportedEncodingException e) {
					message.addRecipient(RecipientType.TO, new InternetAddress(address));
				}
			} else {
				message.addRecipient(RecipientType.TO, new InternetAddress(address));
			}
		}
		for (final String address : ccListBox.getRecipients()) {
			if (addressContacts.containsKey(address)) {
				try {
					message.addRecipient(RecipientType.CC,
							new InternetAddress(address, addressContacts.get(address).getFullname()));
				} catch (final UnsupportedEncodingException e) {
					message.addRecipient(RecipientType.CC, new InternetAddress(address));
				}
			} else {
				message.addRecipient(RecipientType.CC, new InternetAddress(address));
			}
		}
		for (final String address : bccListBox.getRecipients()) {
			if (addressContacts.containsKey(address)) {
				try {
					message.addRecipient(RecipientType.BCC,
							new InternetAddress(address, addressContacts.get(address).getFullname()));
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
		} catch (final MessagingException e) {
			// TODO display error message
			LOGGER.error("building message", e);
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

	public void setOnCompose(VoidCallback<String> callback) {
		composeCallback = callback;
	}
}
