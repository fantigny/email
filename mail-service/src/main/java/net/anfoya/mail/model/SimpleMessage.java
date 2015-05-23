package net.anfoya.mail.model;

import net.anfoya.mail.service.Message;


@SuppressWarnings("serial")
public class SimpleMessage implements Message {

	private final String id;
	private final boolean draft;

	private byte[] raw;

	public SimpleMessage(final String id, final boolean draft, final byte[] rfc822mimeRaw) {
		this.id = id;
		this.draft = draft;
		this.raw = rfc822mimeRaw;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public byte[] getRaw() {
		return raw;
	}

	@Override
	public void setRaw(final byte[] rfc822mimeRaw) {
		if (!isDraft()) {
			throw new RuntimeException("not authorized to update message");
		}
		this.raw = rfc822mimeRaw;
	}

	@Override
	public boolean isDraft() {
		return draft;
	}
}
