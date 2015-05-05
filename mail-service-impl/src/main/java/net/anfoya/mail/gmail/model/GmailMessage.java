package net.anfoya.mail.gmail.model;

import net.anfoya.mail.model.Message;

public class GmailMessage extends Message {

	private static String findBody(final com.google.api.services.gmail.model.Message message) {
		return message.getPayload().getParts().get(0).getBody().getData();
	}

	public GmailMessage(final com.google.api.services.gmail.model.Message message) {
		super(message.getId(), findBody(message));
	}
}
