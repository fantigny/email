package net.anfoya.mail.browser.javafx.thread;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.web.WebViewFitContent;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

public class MessagePane<M extends SimpleMessage> extends VBox {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessagePane.class);
	private static final Session SESSION = Session.getDefaultInstance(new Properties(), null);
	private static final String EMPTY = "[empty]";
	private static final String UNKNOWN = "[unknown]";

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService;

	private final BooleanProperty expanded;
	private boolean collapsible;

	private final Text titleText;
	private final WebViewFitContent bodyView;

	private final String messageId;
	private M message;
	private MimeMessage mimeMessage;
	private Task<String> loadTask;

	public MessagePane(final String messageId, final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService) {
		this.mailService = mailService;
		this.messageId = messageId;
		this.collapsible = true;

		bodyView = new WebViewFitContent();

		expanded = new SimpleBooleanProperty(true);
		expanded.addListener((ov, oldVal, newVal) -> {
			if (collapsible) {
				final double height = newVal? bodyView.getPrefHeight(): 0;
				bodyView.setMinHeight(height);
				bodyView.setMaxHeight(height);
				autosize();
			}
		});

		final HBox titlePane = new HBox();
		titlePane.setPadding(new Insets(5));
		titlePane.setAlignment(Pos.CENTER_LEFT);
		titlePane.setMinHeight(30);
		titlePane.setOnMouseClicked(event -> {
			expanded.set(!expanded.get());
		});

		titleText = new Text("loading...");
		titlePane.getChildren().add(titleText);

		getChildren().addAll(titlePane, bodyView);
		setOnDragDetected(event -> {
	        final ClipboardContent content = new ClipboardContent();
	        content.put(ThreadDropPane.MESSAGE_DATA_FORMAT, message);
	        final Dragboard db = startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
	}

	public synchronized void load() {
		if (loadTask != null) {
			//already loading;
			return;
		}
		loadTask = new Task<String>() {
			@Override
			protected String call() throws MailException, MessagingException, IOException {
				message = mailService.getMessage(messageId);
			    mimeMessage = new MimeMessage(SESSION, new ByteArrayInputStream(message.getRfc822mimeRaw()));
			    return toHtml(mimeMessage);
			}
		};
		loadTask.setOnFailed(event -> {
			LOGGER.error("loading message id {}", messageId, event.getSource().getException());
		});
		loadTask.setOnSucceeded(event -> {
			refreshTitle();
			bodyView.loadContent(loadTask.getValue());
		});
		ThreadPool.getInstance().submitHigh(loadTask);
	}

	protected String toHtml(final MimeMessage message) throws MessagingException, IOException {
		final boolean isHtml = message.getContentType().contains("multipart/alternative");
		String html = toHtml(message.getContent(), message.getContentType(), message.getFileName(), isHtml);
		if (html.isEmpty() || !isHtml) {
			html = "<html><body><pre>" + html + "</pre></body></html>";
		}
		return html;
	}

	private String toHtml(final Object content, String type, String filename, boolean isHtml) throws IOException, MessagingException {
		isHtml = isHtml || type.contains("multipart/alternative");
		type = type.replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", " ");
		if (type.contains("text/html")) {
			LOGGER.debug("part type {}", type);
			return (String) content;
		} else if (type.contains("text/plain") && !isHtml) {
			LOGGER.debug("part type {}", type);
			return (String) content;
		} else if (content instanceof MimeBodyPart) {
			LOGGER.debug("part type {}", type);
			final MimeBodyPart part = (MimeBodyPart) content;
			return toHtml(part.getContent(), part.getContentType(), part.getFileName(), isHtml);
		} else if (content instanceof Multipart) {
			LOGGER.debug("part type {}", type);
			final Multipart parts = (Multipart) content;
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				final BodyPart part = parts.getBodyPart(i);
				html.append(toHtml(part.getContent(), part.getContentType(), part.getFileName(), isHtml));
			}
			return html.toString();
		} else if (content instanceof BASE64DecoderStream) {
			filename = MimeUtility.decodeText(filename);
			LOGGER.debug("save file {}", filename);
			save((BASE64DecoderStream) content, filename);
			return "";
		} else {
			LOGGER.warn("skip type {}", type, content.getClass().getName());
			return "";
		}
	}

	private void save(final BASE64DecoderStream stream, final String name) throws IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(name));
			String line;
			while((line=reader.readLine()) != null) {
				writer.write(line);
			}
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	private void refreshTitle() {
		try {
			final StringBuilder title = new StringBuilder();
			title.append(mimeMessage.getSentDate());
			title.append(" from ").append(getMailAddresses(mimeMessage.getFrom()));
			title.append(" to ").append(getMailAddresses(mimeMessage.getRecipients(Message.RecipientType.TO)));;
			titleText.setText(title.toString());
		} catch (final MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getMailAddresses(final Address[] addresses) {
		final StringBuilder sb = new StringBuilder();
		if (addresses == null || addresses.length == 0) {
			sb.append(EMPTY);
		} else {
			boolean first = true;
			for(final Address address: addresses) {
				if (!first) {
					sb.append(", ");
				}
				first = false;

				if (address.getType().equalsIgnoreCase("rfc822")) {
					final InternetAddress mailAddress = (InternetAddress) address;
					if (mailAddress.getPersonal() != null) {
						sb.append(mailAddress.getPersonal());
					} else {
						sb.append(mailAddress.getAddress());
					}
				} else {
					sb.append(UNKNOWN);
				}
			}
		}

		return sb.toString();
	}

	public void setScrollHandler(final EventHandler<ScrollEvent> handler) {
		bodyView.setScrollHandler(handler);
	}

	public boolean isExpanded() {
		return expanded.get();
	}

	public void setExpanded(final boolean expanded) {
		this.expanded.set(expanded);
	}

	public BooleanProperty expandedProperty() {
		return expanded;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setCollapsible(final boolean collapsible) {
		this.collapsible = collapsible;
	}

	public boolean isCollapsble() {
		return collapsible;
	}
}
