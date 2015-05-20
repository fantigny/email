package net.anfoya.mail.model;

import net.anfoya.mail.service.Message;


@SuppressWarnings("serial")
public class SimpleMessage implements Message {

	private final String id;
	private final byte[] rfc822mimeRaw;
	private final boolean draft;

	public SimpleMessage(final String id, final byte[] rfc822mimeRaw, final boolean draft) {
		this.id = id;
		this.rfc822mimeRaw = rfc822mimeRaw;
		this.draft = draft;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public byte[] getRfc822mimeRaw() {
		return rfc822mimeRaw;
	}

	@Override
	public boolean isDraft() {
		return draft;
	}
}
