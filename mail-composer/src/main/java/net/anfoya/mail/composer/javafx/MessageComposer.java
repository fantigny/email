package net.anfoya.mail.composer.javafx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.AutoShowComboBoxListener;
import net.anfoya.javafx.scene.control.ComboField;
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

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

public class MessageComposer<M extends Message, C extends Contact> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageComposer.class);
	private static final FontLoader FONT_LOADER = Toolkit.getToolkit().getFontLoader();

	private final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService;
	private final EventHandler<ActionEvent> updateHandler;
	private final MessageHelper helper;

	private final BorderPane mainPane;
	private final GridPane headerPane;
	private final HBox toLabelBox;

	private final HTMLEditor bodyEditor;
	private final ComboBox<String> fromCombo;
	private final TextField subjectField;

	private final FlowPane toListPane;
	private final ComboField<String> toCombo;
	private final FlowPane ccListPane;
	private final ComboField<String> ccCombo;
	private final FlowPane bccListPane;
	private final ComboField<String> bccCombo;

	private Button sendButton;

	private M draft;

	public MessageComposer(final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService, final EventHandler<ActionEvent> updateHandler) {
		super(StageStyle.UNIFIED);
		setTitle("FisherMail / Agaar / Agamar / Agaram");

		final Image icon = new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png"));
		getIcons().add(icon);

		final Scene scene = new Scene(new BorderPane(), 800, 600);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/mail/css/Mail.css").toExternalForm());
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

		fromCombo = new ComboBox<String>();
		fromCombo.getItems().add("me");
		fromCombo.getSelectionModel().select(0);
		fromCombo.setDisable(true);

		toLabelBox = new HBox(new Label("to"), new Label(" ..."));
		toLabelBox.setAlignment(Pos.CENTER_LEFT);
		toLabelBox.setOnMouseClicked(event -> {
			if (headerPane.getChildren().contains(fromCombo)) {
				toMiniHeader();
			} else {
				toFullHeader();
			}
		});

		float minWidth = -1f;
		final Map<String, C> emailContacts = new LinkedHashMap<String, C>();
		try {
			for(final C c: mailService.getContacts()) {
				emailContacts.put(c.getEmail(), c);

		    	final float stringWidth = FONT_LOADER.computeStringWidth(c.getFullname(), new Label().getFont());
		    	if (minWidth < stringWidth) {
		    		minWidth = stringWidth;
		    	}
			}
		} catch (final MailException e) {
			LOGGER.error("loading contacts", e);
		}

		subjectField = new TextField("FisherMail - test");
		subjectField.setStyle("-fx-background-color: #cccccc");

		final Callback<String, String> contactDisplay = email -> {
			return emailContacts.get(email).getFullname() + " <" + email + ">";
		};

		final Callback<ListView<String>, ListCell<String>> addressCellFactory = listView -> {
			return new ListCell<String>() {
				@Override
			    public void updateItem(final String email, final boolean empty) {
			        super.updateItem(email, empty);
			        if (!empty) {
			        	final String display = contactDisplay.call(email);
			        	setText(display);

			        	final double listPrefWidth = FONT_LOADER.computeStringWidth(display, getFont());
			        	if (listView.getPrefWidth() < listPrefWidth) {
			        		listView.setPrefWidth(listPrefWidth);
			        	}
			        }
				}
			};
		};


		toCombo = new ComboField<String>();
		toCombo.setEditable(true);
		toCombo.getItems().setAll(emailContacts.keySet());
		toCombo.setCellFactory(addressCellFactory);

		toListPane = new FlowPane(3, 2);
		toListPane.setMinWidth(150);
		toListPane.getChildren().add(toCombo);
		toListPane.widthProperty().addListener((ov, o, n) -> organizeListPane(toListPane));
		toCombo.setOnFieldAction(e -> addContact(toListPane, toCombo.getFieldValue()));

		new AutoShowComboBoxListener(toCombo, email -> contains(toListPane, email)? "": contactDisplay.call(email));
		organizeListPane(toListPane);


		ccCombo = new ComboField<String>();
		ccCombo.setEditable(true);
		ccCombo.getItems().setAll(emailContacts.keySet());
		ccCombo.setCellFactory(addressCellFactory);

		ccListPane = new FlowPane(3, 2);
		ccListPane.setMinWidth(150);
		ccListPane.getChildren().add(ccCombo);
		ccCombo.setOnFieldAction(e -> addContact(ccListPane, ccCombo.getFieldValue()));

		new AutoShowComboBoxListener(ccCombo, email -> contains(ccListPane, email)? "": contactDisplay.call(email));
		organizeListPane(ccListPane);


		bccCombo = new ComboField<String>();
		bccCombo.setEditable(true);
		bccCombo.getItems().setAll(emailContacts.keySet());
		bccCombo.setCellFactory(addressCellFactory);

		bccListPane = new FlowPane(3, 2);
		bccListPane.setMinWidth(150);
		bccListPane.getChildren().add(bccCombo);
		bccCombo.setOnFieldAction(e -> addContact(bccListPane, bccCombo.getFieldValue()));

		new AutoShowComboBoxListener(bccCombo, email -> contains(bccListPane, email)? "": contactDisplay.call(email));
		organizeListPane(bccListPane);

		toMiniHeader();

		bodyEditor = new HTMLEditor();
		bodyEditor.setPadding(new Insets(0, 0, 0, 5));
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

		sendButton = new Button("send");
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

	@SuppressWarnings("unchecked")
	private void organizeListPane(final FlowPane pane) {
		final double paneWidth = subjectField.getWidth();
		LOGGER.debug("pane width {}", paneWidth);

		double availableWidth = paneWidth;
		ComboField<String> combo = null;
		for(final Node node: pane.getChildren()) {
			if (node instanceof Label) {
				final Label l = (Label) node;

				final double stringWidth = Math.ceil(FONT_LOADER.computeStringWidth(l.getText(), l.getFont()))
						+ l.getPadding().getLeft() + l.getPadding().getRight();

				if (stringWidth > availableWidth) {
					availableWidth = paneWidth;
				}
				availableWidth -= stringWidth + pane.getHgap();
			} else if (node instanceof ComboBox) {
				combo = (ComboField<String>) node;
			}
		}
		LOGGER.debug("available width {}", availableWidth);

		combo.hide();
		combo.setFieldValue("");

		if (availableWidth > 150) {
			combo.prefWidthProperty().bind(subjectField.widthProperty().add(availableWidth - paneWidth));
		} else {
			combo.prefWidthProperty().bind(subjectField.widthProperty());
		}
	}

	private boolean contains(final FlowPane pane, final String contact) {
		final String l = getLabel(contact);
		for (final Node n : pane.getChildren()) {
			if (n instanceof Label && ((Label) n).getText().equals(l)) {
				return true;
			}
		}
		return false;
	}

	private String getLabel(final String contact) {
		return contact + " X";
	}

	private void addContact(final FlowPane pane, final String contact) {
		if (contact == null || contact.isEmpty()) {
			return;
		}
		final Label label = new Label(getLabel(contact));
		label.getStyleClass().add("address-label");
		label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
		label.setOnMouseClicked(e -> removeContact(pane, label));
		for(final Iterator<Node> i = pane.getChildren().iterator(); i.hasNext();) {
			final Node n = i.next();
			if (n instanceof Label && ((Label) n).getText().equals(label.getText())) {
				// remove address if already in the list
				i.remove();
			}
		}
		LOGGER.debug("elements in flow pane: {}", pane.getChildren().size());
		pane.getChildren().add(pane.getChildren().size() - 1, label);

		organizeListPane(pane);
	}

	private void removeContact(final FlowPane pane, final Label label) {
		pane.getChildren().remove(label);
		organizeListPane(pane);
	}

	public void newMessage(final String recipient) throws MailException {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				draft = mailService.createDraft(null);
				toCombo.setValue(recipient);
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
			for(final String c: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.TO))) {
				addContact(toListPane, c);
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading recipients");
		}

		try {
			for(final String c: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.CC))) {
				addContact(ccListPane, c);
			}
		} catch (final MessagingException e) {
			LOGGER.error("reading cc list");
		}

		try {
			for(final String c: helper.getMailAddresses(message.getRecipients(MimeMessage.RecipientType.BCC))) {
				addContact(bccListPane, c);
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

		for(final Node n: toListPane.getChildren()) {
			if (n instanceof Label) {
				final String t = ((Label) n).getText();
				final String a = t.substring(0, t.length() - 2);
				final InternetAddress to = new InternetAddress(a);
				message.addRecipient(RecipientType.TO, to);
			}
		}

		for(final Node n: ccListPane.getChildren()) {
			if (n instanceof Label) {
				final String t = ((Label) n).getText();
				final String a = t.substring(0, t.length() - 2);
				final InternetAddress to = new InternetAddress(a);
				message.addRecipient(RecipientType.CC, to);
			}
		}

		for(final Node n: bccListPane.getChildren()) {
			if (n instanceof Label) {
				final String t = ((Label) n).getText();
				final String a = t.substring(0, t.length() - 2);
				final InternetAddress to = new InternetAddress(a);
				message.addRecipient(RecipientType.BCC, to);
			}
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
