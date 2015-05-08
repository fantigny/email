package net.anfoya.mail.browser.javafx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

public class MessagePane extends TitledPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessagePane.class);

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread> mailService;

	private final WebView bodyView;

	private MimeMessage message;

	public MessagePane(final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread> mailService) {
		super("loading...", new BorderPane());
		this.mailService = mailService;

		final BorderPane mainPane = (BorderPane) getContent();
		mainPane.setPadding(new Insets(0));

		bodyView = new WebView();
		mainPane.setCenter(bodyView);
	}

	public void load(final String messageId) {
		clear();

		final Task<MimeMessage> task = new Task<MimeMessage>() {
			@Override
			protected MimeMessage call() throws Exception {
				return mailService.getMessage(messageId);
			}
		};
		task.setOnSucceeded(event -> {
			try {
				this.message = task.get();
				load();
			} catch (final Exception e) {
				LOGGER.error("loading message id: " + messageId, e);
			}
		});
		ThreadPool.getInstance().submit(task);
	}

	private void load() throws IOException, MessagingException {
		loadTitle();
		loadBody();
	}

	private void loadTitle() throws MessagingException {
		final StringBuilder title = new StringBuilder();
		title.append(message.getSentDate());
		title.append(" from ").append(getMailAddress(message.getFrom()[0]));
		title.append(" to ");
		boolean multiple = false;
		for(final Address a: message.getRecipients(Message.RecipientType.TO)) { //TODO nullpointer
			if (multiple) {
				title.append(", ");
			}
			title.append(getMailAddress(a));
			multiple = true;
		}
		setText(title.toString());
	}

	//TODO replace with getMailAddress*es*
	private String getMailAddress(final Address address) {
		if (address != null && address.getType().equalsIgnoreCase("rfc822")) {
			final InternetAddress mailAddress = (InternetAddress) address;
			if (mailAddress.getPersonal() != null) {
				return mailAddress.getPersonal();
			} else {
				return mailAddress.getAddress();
			}
		} else {
			return "[unknown]";
		}

	}

	private void loadBody() throws IOException, MessagingException {
		String html = toHtml(message.getContent(), message.getContentType(), false);
		if (html.isEmpty()) {
			html = toHtml(message.getContent(), message.getContentType(), true);
			html = "<html><body><pre>" + html + "</pre></body></html>";
		}
		bodyView.getEngine().loadContent(html);
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
		bodyView.getEngine().loadContent("");
	}
}
