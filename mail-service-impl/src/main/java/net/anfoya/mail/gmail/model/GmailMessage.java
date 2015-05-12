package net.anfoya.mail.gmail.model;

import net.anfoya.mail.model.SimpleMessage;

@SuppressWarnings("serial")
public class GmailMessage extends SimpleMessage {

	public GmailMessage(final String id, final byte[] rfc822mimeRaw) {
		super(id, rfc822mimeRaw);
	}
}
