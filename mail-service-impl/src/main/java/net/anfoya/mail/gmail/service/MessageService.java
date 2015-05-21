package net.anfoya.mail.gmail.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import net.anfoya.java.cache.FileSerieSerializedMap;
import net.anfoya.mail.gmail.cache.CacheData;
import net.anfoya.mail.gmail.cache.CacheException;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

public class MessageService {
	private final Map<String, CacheData<Message>> idMessages;
	private final Gmail gmail;
	private final String user;

	public MessageService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		idMessages = new FileSerieSerializedMap<String, CacheData<Message>>("id-messages", 50);
	}

	public Message getMessage(final String id) throws MessageException {
		Message message = null;
		if (idMessages.containsKey(id)) {
			try {
				message = idMessages.get(id).getData();
			} catch (final CacheException e) {
				idMessages.remove(id);
			}
		}
		if (message == null) {
			try {
				message = gmail.users().messages().get(user, id).setFormat("raw").execute();
				idMessages.put(id, new CacheData<Message>(message));
			} catch (final IOException e) {
				throw new MessageException("getting message id " + id, e);
			}
		}

		return message;
	}

	public Draft createDraft() throws MessageException {
		try {
			final MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
			mimeMessage.setContent("", "text/html; charset=utf-8");
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mimeMessage.writeTo(baos);
		    final Message message = new Message();
		    message.setRaw(Base64.getUrlEncoder().encodeToString(baos.toByteArray()));
		    final Draft draft = new Draft();
		    draft.setMessage(message);
		    final String id = gmail.users().drafts().create(user, draft).execute().getId();
		    draft.setId(id);
		    return draft;
		} catch (IOException | MessagingException e) {
			throw new MessageException("creating draft", e);
		}
	}

	public void removeDraft(final String id) throws MessageException {
		try {
			gmail.users().drafts().delete(user, id).execute();
		} catch (final IOException e) {
			throw new MessageException("deleting draft id " + id, e);
		}
	}

	public void removeMessage(final String id) throws MessageException {
		try {
			gmail.users().messages().delete(user, id).execute();
		} catch (final IOException e) {
			throw new MessageException("deleting message id " + id, e);
		}
	}
}
