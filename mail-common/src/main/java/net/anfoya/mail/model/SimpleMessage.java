package net.anfoya.mail.model;

import javax.mail.internet.MimeMessage;


@SuppressWarnings("serial")
public abstract class SimpleMessage implements Message {

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
	public void setMimeDraft(final MimeMessage mimeMessage) {
		if (!isDraft()) {
			throw new RuntimeException("message is unmutable, only draft can be updated");
		}
		this.mimeMessage = mimeMessage;
	}
}
