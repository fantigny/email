package net.anfoya.mail.model;

import javax.mail.internet.MimeMessage;

import net.anfoya.mail.service.Message;


@SuppressWarnings("serial")
public class SimpleMessage implements Message {

	private final String id;
	private final boolean draft;

	private transient MimeMessage mimeMessage;

	public SimpleMessage(final String id, final boolean draft, final MimeMessage mimeMessage) {
		this.id = id;
		this.draft = draft;
		this.mimeMessage = mimeMessage;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isDraft() {
		return draft;
	}

	@Override
	public MimeMessage getMimeMessage() {
		return mimeMessage;
	}

	@Override
	public void setMimeMessage(final MimeMessage mimeMessage) {
		if (!isDraft()) {
			throw new RuntimeException("not authorized to update message");
		}
		this.mimeMessage = mimeMessage;
	}
}
