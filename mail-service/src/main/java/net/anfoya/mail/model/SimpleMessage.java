package net.anfoya.mail.model;

import net.anfoya.mail.service.Message;


@SuppressWarnings("serial")
public class SimpleMessage implements Message {

	private final String id;
	private final byte[] rfc822mimeRaw;

	public SimpleMessage(final String id, final byte[] rfc822mimeRaw) {
		this.id = id;
		this.rfc822mimeRaw = rfc822mimeRaw;
	}

	public String getId() {
		return id;
	}

	public byte[] getRfc822mimeRaw() {
		return rfc822mimeRaw;
	}
}
