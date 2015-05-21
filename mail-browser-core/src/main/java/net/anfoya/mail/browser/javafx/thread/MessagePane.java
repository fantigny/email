package net.anfoya.mail.browser.javafx.thread;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
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
	private static final String TEMP = System.getProperty("java.io.tmpdir");
	private static final String ATTACH_ICON_PATH = TEMP + "/fishermail-attachment.png";

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService;

	private final BooleanProperty expanded;
	private boolean collapsible;

	private final Text titleText;
	private final WebViewFitContent bodyView;

	private final String messageId;
	private M message;
	private MimeMessage mimeMessage;
	private Task<String> loadTask;
	private final Map<String, String> cidFilenames;

	private static boolean copied = false;
	{
		if (!copied) {
			copied = true;
			try {
				Files.copy(getClass().getResourceAsStream("attachment.png"), new File(ATTACH_ICON_PATH).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public MessagePane(final String messageId, final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService) {
		this.mailService = mailService;
		this.messageId = messageId;
		this.collapsible = true;

		cidFilenames = new HashMap<String, String>();
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
			//already loaded;
			return;
		}
		loadTask = new Task<String>() {
			@Override
			protected String call() throws MailException, MessagingException, IOException, URISyntaxException {
				message = mailService.getMessage(messageId);
			    mimeMessage = new MimeMessage(SESSION, new ByteArrayInputStream(message.getRfc822mimeRaw()));
			    String html = toHtml(mimeMessage, false);
			    html = replaceCids(html);
			    return html;
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
				&& ((MimeBodyPart)part).getContentID() != null) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String cid = bodyPart.getContentID().replaceAll("<", "").replaceAll(">", "");
			final String tempFilename = TEMP + (part.getFileName() == null? cid: MimeUtility.decodeText(bodyPart.getFileName()));
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			cidFilenames.put(cid, tempFilename);
			return "";
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& part.getDisposition() != null) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String tempFilename = TEMP + MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			if (MimeBodyPart.INLINE.equalsIgnoreCase(part.getDisposition())) {
				return "<img src='file://" + tempFilename + "'>";
			} else {
				return "<a href ='file://" + tempFilename + "'><table><tr><td><img src='file://" + ATTACH_ICON_PATH + "'></td></tr><tr><td>" + MimeUtility.decodeText(bodyPart.getFileName()) + "</td></tr></table></a>";
			}
		} else {
			LOGGER.warn("---- type {}", type);
			return "";
		}
	}

	private String replaceCids(String html) {
		for(final Entry<String, String> entry: cidFilenames.entrySet()) {
			html = html.replaceAll("cid:" + entry.getKey(), "file://" + entry.getValue());
		}
		return html;
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
