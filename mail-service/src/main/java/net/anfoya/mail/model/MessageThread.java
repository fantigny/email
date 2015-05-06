package net.anfoya.mail.model;

import java.util.Set;

public class MessageThread {

	private final String id;
	private final String subject;
	private final Set<String> messageIds;

	public MessageThread(final String id, final String subject, final Set<String> messageIds) {
		this.id = id;
		this.subject = subject;
		this.messageIds = messageIds;
	}

	@Override
	public String toString() {
		return subject;
	}

	public String getId() {
		return id;
	}

	public String getSubject() {
		return subject;
	}

	public Set<String> getMessageIds() {
		return messageIds;
	}
}
