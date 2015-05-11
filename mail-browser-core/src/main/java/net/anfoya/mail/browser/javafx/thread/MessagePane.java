package net.anfoya.mail.browser.javafx.thread;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Set;

import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.web.WebViewFitContent;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

public class MessagePane<M extends SimpleMessage> extends TitledPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessagePane.class);
	private static final Session SESSION = Session.getDefaultInstance(new Properties(), null);

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService;

	private final WebViewFitContent bodyView;
	private final String messageId;

	private M message;
	private MimeMessage mimeMessage;

	public MessagePane(final String messageId, final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService
			, final ScrollPane parentScrollPane) {
		super("loading...", new BorderPane());
		setExpanded(false);

		this.mailService = mailService;
		this.messageId = messageId;

		final BorderPane mainPane = (BorderPane) getContent();
		mainPane.setPadding(new Insets(0));

		bodyView = new WebViewFitContent(parentScrollPane);
		bodyView.getChildrenUnmodifiable().addListener(
				new ListChangeListener<Node>() {
					@Override
					public void onChanged(final Change<? extends Node> change) {
						final Set<Node> deadSeaScrolls = bodyView.lookupAll(".scroll-bar");
						for (final Node scroll : deadSeaScrolls) {
							scroll.setVisible(false);
						}
					}
				});
		bodyView.setContextMenuEnabled(false);
		mainPane.setCenter(bodyView);

		setOnDragDetected(event -> {
	        final ClipboardContent content = new ClipboardContent();
	        content.put(ThreadDropPane.MESSAGE_DATA_FORMAT, message);
	        final Dragboard db = startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
	}

	public void refresh() {
		clear();

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				message = mailService.getMessage(messageId);
			    mimeMessage = new MimeMessage(SESSION, new ByteArrayInputStream(message.getRfc822mimeRaw()));
				return null;
			}
		};
		task.setOnSucceeded(event -> {
			try {
				refreshTitle();
				refreshBody();
			} catch (final Exception e) {
				LOGGER.error("loading message id: " + messageId, e);
			}
		});
		ThreadPool.getInstance().submit(task);
	}

	private void refreshTitle() throws MessagingException {
		final StringBuilder title = new StringBuilder();
		title.append(mimeMessage.getSentDate());
		title.append(" from ").append(getMailAddresses(mimeMessage.getFrom()));
		title.append(" to ").append(getMailAddresses(mimeMessage.getRecipients(Message.RecipientType.TO)));;
		setText(title.toString());
	}

	private String getMailAddresses(final Address[] addresses) {
		final StringBuilder sb = new StringBuilder();
		if (addresses == null || addresses.length == 0) {
			sb.append("[empty]");
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
					sb.append("[unknown]");
				}
			}
		}

		return sb.toString();
	}

	private void refreshBody() throws MessagingException, IOException {
		String html = toHtml(mimeMessage.getContent(), mimeMessage.getContentType(), false);
		if (html.isEmpty()) {
			html = toHtml(mimeMessage.getContent(), mimeMessage.getContentType(), true);
			html = "<html><body><pre>" + html + "</pre></body></html>";
		}
		bodyView.loadContent(html);
	}

	private String toHtml(final Object mimeContent, final String mimeType, final boolean allowText) throws MessagingException, IOException {
		if (mimeContent instanceof String && (mimeType.toLowerCase().contains("html") || allowText)) {
			return (String) mimeContent;
		} else if (mimeContent instanceof MimeBodyPart) {
			final MimeBodyPart part = (MimeBodyPart) mimeContent;
			return toHtml(part.getContent(), part.getContentType(), allowText);
		} else if (mimeContent instanceof Multipart) {
			final Multipart parts = (Multipart) mimeContent;
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				final BodyPart part = parts.getBodyPart(i);
				html.append(toHtml(part.getContent(), part.getContentType(), allowText));
			}
			return html.toString();
		} else if (mimeContent instanceof BASE64DecoderStream) {
			final BASE64DecoderStream decoderStream = (BASE64DecoderStream) mimeContent;
			final BufferedReader reader = new BufferedReader(new InputStreamReader(decoderStream));
			final StringBuilder content = new StringBuilder();
			String line;
			while((line=reader.readLine()) != null) {
				content.append(line);
			}
			return toHtml(content.toString(), mimeType, allowText);
		} else {
			LOGGER.warn("no handler for class {} and type {}", mimeContent.getClass().getName(), mimeType);
			return "";
		}
	}

	public void clear() {
		bodyView.clear();
	}

	public M getMessage() {
		return message;
	}
}
