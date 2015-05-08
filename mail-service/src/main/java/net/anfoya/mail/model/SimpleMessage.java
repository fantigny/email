package net.anfoya.mail.model;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SimpleMessage implements Serializable {

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
