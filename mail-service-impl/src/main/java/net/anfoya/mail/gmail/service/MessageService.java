package net.anfoya.mail.gmail.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

import net.anfoya.java.cache.FileSerieSerializedMap;
import net.anfoya.mail.gmail.cache.CacheData;

public class MessageService {
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-cache-id-messages-";

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
				throw new MessageException("get message " + id, e);
			}
		}

		return message;
	}

	public void removeMessage(final String id) throws MessageException {
		try {
			gmail.users().messages().delete(user, id).execute();
		} catch (final IOException e) {
			throw new MessageException("delete message " + id, e);
		}
	}

	public void send(final String id, final String raw) throws MessageException {
		try {
			final Message message = new Message();
			message.setRaw(raw);
			final Draft draft = getDraft(id);
			draft.setMessage(message);
			gmail.users().drafts().send(user, draft).execute();
		} catch (final IOException e) {
			throw new MessageException("send draft " + id, e);
		}
	}

	public void save(final String id, final String raw) throws MessageException {
		try {
			final Message message = new Message();
			message.setRaw(raw);
			final Draft draft = getDraft(id);
			draft.setMessage(message);
			gmail.users().drafts().update(user, id, draft).execute();
		} catch (final IOException e) {
			throw new MessageException("save draft " + id, e);
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
			throw new MessageException("create draft", e);
		}
	}

	public void removeDraft(final String id) throws MessageException {
		try {
			gmail.users().drafts().delete(user, id).execute();
		} catch (final IOException e) {
			throw new MessageException("delete draft " + id, e);
		}
	}

	public Draft getDraftForMessage(final String id) throws MessageException {
		try {
			Draft draft = null;
			for(final Draft d: gmail.users().drafts().list(user).execute().getDrafts()) {
				if (d.getMessage() != null && d.getMessage().getId().equals(id)) {
					draft = getDraft(d.getId());
					break;
				}
			}
			return draft;
		} catch (final IOException e) {
			throw new MessageException("get draft for message " + id, e);
		}
	}

	public Draft getDraft(final String id) throws MessageException {
		try {
			return gmail.users().drafts().get(user, id).setFormat("raw").execute();
		} catch (final IOException e) {
			throw new MessageException("get draft " + id, e);
		}
	}

	public void clearCache() {
		idMessages.clear();
	}
}
