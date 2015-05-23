package net.anfoya.mail.gmail.model;

import java.util.Base64;

import net.anfoya.mail.model.SimpleMessage;

import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

@SuppressWarnings("serial")
public class GmailMessage extends SimpleMessage {

	public GmailMessage(final Message message) {
		super(message.getId(), false, Base64.getUrlDecoder().decode(message.getRaw()));
	}

	public GmailMessage(final Draft draft) {
		super(draft.getId(), true, Base64.getUrlDecoder().decode(draft.getMessage().getRaw()));
	}
}
