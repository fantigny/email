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

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

public class MessageService {
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + "/fsm-cache-id-messages-";

	private final Map<String, CacheData<Message>> idMessages;
	private final Gmail gmail;
	private final String user;

	public MessageService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		idMessages = new FileSerieSerializedMap<String, CacheData<Message>>(FILE_PREFIX + user, 50);
	}

	public Message getMessage(final String id) throws MessageException {
		Message message = null;
		if (idMessages.containsKey(id)) {
			try {
				message = idMessages.get(id).getData();
			} catch (final Exception e) {
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

	public void removeMessage(final String id) throws MessageException {
		try {
			gmail.users().messages().delete(user, id).execute();
		} catch (final IOException e) {
			throw new MessageException("deleting message id " + id, e);
		}
	}

	public void send(final String draftId, final byte[] raw) throws MessageException {
		try {
			final Message message = new Message();
			message.setRaw(new String(raw));
			final Draft draft = gmail.users().drafts().get(user, draftId).execute();
			draft.setMessage(message);
			gmail.users().drafts().send(user, draft).execute();
		} catch (final IOException e) {
			throw new MessageException("sending message id " + draftId, e);
		}
	}

	public void save(final String id, final byte[] raw) throws MessageException {
		try {
			final Message message = new Message();
			message.setRaw(new String(raw));
			final Draft draft = new Draft();
			draft.setId(id);
			draft.setMessage(message);
			gmail.users().drafts().update(user, id, draft).execute();
		} catch (final IOException e) {
			throw new MessageException("saving draft " + id, e);
		}
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

	public Draft getDraftForMessage(final String id) throws MessageException {
		try {
			Draft draft = null;
			for(final Draft d: gmail.users().drafts().list(user).execute().getDrafts()) {
				if (d.getMessage() != null && id.equals(d.getMessage().getId())) {
					draft = d;
					draft.setMessage(getMessage(id));
					break;
				}
			}
			return draft;
		} catch (final IOException e) {
			throw new MessageException("getting draft id " + id, e);
		}
	}
}
