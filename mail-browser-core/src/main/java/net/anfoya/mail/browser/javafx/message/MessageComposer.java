package net.anfoya.mail.browser.javafx.message;

import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import net.anfoya.mail.browser.javafx.message.ACComboBox1.AutoCompleteMode;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

public class MessageComposer<M extends SimpleMessage> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageComposer.class);
	private static final String TEMP = System.getProperty("java.io.tmpdir") + "/";

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService;

	private final HTMLEditor editor;
	private final ComboBox<String> fromCombo;
	private final ComboBox<String> toCombo;
	private final TextField ccField;
	private final TextField bccField;
	private final TextField subjectField;
	private final GridPane headerPane;
	private final HBox toBox;
	private final BorderPane mainPane;
	private final Label toLabel;
	private M draft;

	public MessageComposer(final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService) {
		super(StageStyle.UNIFIED);
		setTitle("FisherMail / Agaar / Agamar / Agaram");
		getIcons().add(new Image(getClass().getResourceAsStream("../entrypoint/Mail.png")));
		setScene(new Scene(new BorderPane(), 800, 600));

		this.mailService = mailService;
		this.mainPane = (BorderPane) getScene().getRoot();
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

		toLabel = new Label("to");
		final Label moreLabel = new Label(" ...");
		toBox = new HBox(toLabel, moreLabel);
		toBox.setAlignment(Pos.CENTER_LEFT);
		toBox.setOnMouseClicked(event -> {
			if (headerPane.getChildren().contains(fromCombo)) {
				toMiniHeader();
			} else {
				toFullHeader();
			}
		});

		final ObservableList<String> contacts = FXCollections.observableArrayList();
		try {
			contacts.addAll(mailService.getContactAddresses());
		} catch (final MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		toCombo = new ComboBox<String>(contacts);
		toCombo.prefWidthProperty().bind(widthProperty());
		toCombo.setEditable(true);
		ACComboBox1.autoCompleteComboBox(toCombo, AutoCompleteMode.CONTAINING);

		ccField = new TextField();
		bccField = new TextField();
		subjectField = new TextField("FisherMail - test");

		toMiniHeader();

		editor = new HTMLEditor();
		mainPane.setCenter(editor);

		final Button discardButton = new Button("discard");
		discardButton.setOnAction(event -> discardAndClose());

		final Button saveButton = new Button("save");
		saveButton.setOnAction(event -> saveAndClose());

		final Button sendButton = new Button("send");
		sendButton.setOnAction(event -> sendAndClose());

		final HBox buttonBox = new HBox(5, discardButton, saveButton, sendButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(5));
		mainPane.setBottom(buttonBox);

		show();
	}

	public void newMessage() throws MailException {
		draft = mailService.createDraft(null);
	}

	public void edit(final M draft) {
		try {
			this.draft = draft;
			editor.setHtmlText(toHtml(draft.getMimeMessage(), false));
		} catch (final MessagingException | IOException e) {
			LOGGER.error("editing draft", e);
		}
	}

	public void reply(final M message, final boolean all) {
		try {
			draft = mailService.createDraft(message);
			final MimeMessage mimeMessage = message.getMimeMessage();

			toCombo.setValue(((InternetAddress)mimeMessage.getFrom()[0]).getAddress());
			subjectField.setText("Re: " + mimeMessage.getSubject());

			final StringBuffer html = new StringBuffer("<br><br>");
			html.append("<blockquote class='gmail_quote' style='margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex'>");
			html.append(toHtml(mimeMessage, false));
			html.append("</blockquote>");
			editor.setHtmlText(html.toString());
			editor.requestFocus();
		} catch (final MessagingException | IOException | MailException e) {
			LOGGER.error("replying message", e);
		}
	}

	public void forward(final M message) {
		try {
			draft = mailService.createDraft(message);
			final MimeMessage mimeMessage = message.getMimeMessage();

			subjectField.setText("Fwd: " + mimeMessage.getSubject());

			final StringBuffer html = new StringBuffer("<br><br>");
			html.append("<blockquote class='gmail_quote' style='margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex'>");
			html.append(toHtml(mimeMessage, false));
			html.append("</blockquote>");
			editor.setHtmlText(html.toString());
			editor.requestFocus();
		} catch (final MessagingException | IOException | MailException e) {
			LOGGER.error("replying message", e);
		}
	}

	private MimeMessage buildMessage() throws MessagingException {
		final MimeMessage mimeMessage = draft.getMimeMessage();
//		mimeMessage.setFrom(new InternetAddress("frederic.antigny@gmail.com"));
		mimeMessage.setSubject(subjectField.getText());

		final MimeMultipart multipart = new MimeMultipart();
		mimeMessage.setContent(multipart);

		final MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(editor.getHtmlText(), "text/html");
		multipart.addBodyPart(mimeBodyPart);

		InternetAddress to;
		try {
			to = new InternetAddress(toCombo.getValue());
			mimeMessage.addRecipient(RecipientType.TO, to);
		} catch (final Exception e) {
			LOGGER.info("no recipient for draft");
		}

		return mimeMessage;
	}

	private void sendAndClose() {
		try {
		    draft.setMimeMessage(buildMessage());
			mailService.send(draft);
		} catch (final MessagingException | MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		close();
	}

	private void saveAndClose() {
	    try {
		    draft.setMimeMessage(buildMessage());
			mailService.save(draft);
		} catch (MessagingException | MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    close();
	}

	private void discardAndClose() {
		try {
			mailService.remove(draft);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		close();
	}

	private void toMiniHeader() {
		headerPane.getChildren().clear();
		headerPane.addRow(0, toBox, toCombo);
		headerPane.addRow(1, new Label("subject"), subjectField);
	}

	private void toFullHeader() {
		headerPane.getChildren().clear();
//		headerPane.addRow(0, new Label("from"), fromCombo);
		headerPane.addRow(0, toLabel, toCombo);
		headerPane.addRow(1, new Label("cc"), ccField);
		headerPane.addRow(2, new Label("bcc"), bccField);
		headerPane.addRow(3, new Label("subject"), subjectField);
	}

	private String toHtml(final Part part, boolean isHtml) throws IOException, MessagingException {
		final String type = part.getContentType().replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", " ");
		isHtml = isHtml || type.contains("multipart/alternative");
		if (part.getContent() instanceof String && type.contains("text/html")) {
			LOGGER.debug("++++ type {}", type);
			return (String) part.getContent();
		} else if (part.getContent() instanceof String && type.contains("text/plain") && !isHtml) {
			LOGGER.debug("++++ type {}", type);
			return "<pre>" + part.getContent() + "</pre>";
		} else if (part instanceof Multipart || type.contains("multipart")) {
			LOGGER.debug("++++ type {}", type);
			final Multipart parts = (Multipart) part.getContent();
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				html.append(toHtml(parts.getBodyPart(i), isHtml));
			}
			return html.toString();
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& part.getDisposition() != null
				&& MimeBodyPart.INLINE.equalsIgnoreCase(part.getDisposition())) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String tempFilename = TEMP + MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			return "<img src='file://" + tempFilename + "'>";
		} else {
			LOGGER.warn("---- type {}", type);
			return "";
		}
	}
}
