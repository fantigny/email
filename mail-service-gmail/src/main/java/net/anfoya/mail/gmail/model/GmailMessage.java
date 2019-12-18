package net.anfoya.mail.gmail.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

import net.anfoya.mail.model.SimpleMessage;

@SuppressWarnings("serial")
public class GmailMessage extends SimpleMessage {
	private static final Session SESSION = Session.getDefaultInstance(new Properties(), null);

	public static MimeMessage getMimeMessage(final Message message) throws MessagingException {
		final byte[] buf = Base64.getUrlDecoder().decode(message.getRaw());
		return new MimeMessage(SESSION, new ByteArrayInputStream(buf));
	}

	public static String toRaw(MimeMessage mimeMessage) throws IOException, MessagingException {
		try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
			mimeMessage.writeTo(bos);
			return new String(Base64.getUrlEncoder().encode(bos.toByteArray()));
		}
	}

	private final String snippet;

	public GmailMessage(final Message message) throws MessagingException {
		super(message.getId(), false, getMimeMessage(message));
		snippet = message.getSnippet();
	}

	public GmailMessage(final Draft draft) throws MessagingException {
		super(draft.getId(), true, getMimeMessage(draft.getMessage()));
		snippet = draft.getMessage().getSnippet();
	}

	@Override
	public String getSnippet() {
		return snippet;
	}

	public String getRaw() throws IOException, MessagingException {
		return toRaw(getMimeMessage());
	}
}
