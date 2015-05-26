package net.anfoya.mail.gmail.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import net.anfoya.mail.model.SimpleMessage;

import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

@SuppressWarnings("serial")
public class GmailMessage extends SimpleMessage {
	private static final Session SESSION = Session.getDefaultInstance(new Properties(), null);

	private static MimeMessage getMimeMessage(final Message message) throws MessagingException {
		final byte[] buf = Base64.getUrlDecoder().decode(message.getRaw());
		return new MimeMessage(SESSION, new ByteArrayInputStream(buf));
	}

	public GmailMessage(final Message message) throws MessagingException {
		super(message.getId(), false, getMimeMessage(message));
	}

	public GmailMessage(final Draft draft) throws MessagingException {
		super(draft.getId(), true, getMimeMessage(draft.getMessage()));
	}

	public String getRaw() throws IOException, MessagingException {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		getMimeMessage().writeTo(bos);
		return new String(Base64.getUrlEncoder().encode(bos.toByteArray()));
	}
}
