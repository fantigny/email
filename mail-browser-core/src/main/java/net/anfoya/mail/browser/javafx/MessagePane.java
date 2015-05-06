package net.anfoya.mail.browser.javafx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
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

	public MessagePane(final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread> mailService) {
		super("loading...", new BorderPane());
		setPadding(new Insets(0));

		this.mailService = mailService;

		bodyView = new WebView();
		((BorderPane)getContent()).setCenter(bodyView);
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
				display(task.get());
			} catch (final Exception e) {
				LOGGER.error("loading message id: " + messageId, e);
			}
		});
		ThreadPool.getInstance().submit(task);
	}

	private void display(final MimeMessage message) throws IOException, MessagingException {
		bodyView.getEngine().loadContent(toHtml(message.getContent(), message.getContentType()));
		setText(new SimpleDateFormat().format(message.getSentDate()));
	}

	private String toHtml(final Object mimeContent, final String mimeType) throws MessagingException, IOException {
		if (mimeContent instanceof String && mimeType.contains("html")) {
			return (String) mimeContent;
		} else if (mimeContent instanceof MimeBodyPart) {
			final MimeBodyPart part = (MimeBodyPart) mimeContent;
			return toHtml(part.getContent(), part.getContentType());
		} else if (mimeContent instanceof Multipart) {
			final Multipart parts = (Multipart) mimeContent;
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				final BodyPart part = parts.getBodyPart(i);
				html.append(toHtml(part.getContent(), part.getContentType()));
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
			return toHtml(content.toString(), mimeType);
		} else {
			LOGGER.warn("no handler for class {} and type {}", mimeContent.getClass().getName(), mimeType);
			return "";
		}
	}

	public void clear() {
		bodyView.getEngine().loadContent("");
	}
}
