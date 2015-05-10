package net.anfoya.mail.gmail;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

public class MessageService {
	private final Map<String, byte[]> idMessages = new ConcurrentHashMap<String, byte[]>();
	private final Gmail gmail;
	private final String user;
	
	public MessageService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;
	}

	public byte[] getRaw(final String id) throws MessageException {
		try {
			if (!idMessages.containsKey(id)) {
				final Message message = gmail.users().messages().get(user, id).setFormat("raw").execute();
				idMessages.put(id,  Base64.getUrlDecoder().decode(message.getRaw()));
			}
			return idMessages.get(id);
		} catch (final IOException e) {
			throw new MessageException("getting message id " + id, e);
		}
	}
}
