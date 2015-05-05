package net.anfoya.mail.model;

import java.util.Set;


public class Thread {

	private final String id;
	private final String object;
	private final Set<String> messageIds;

	public Thread(final String id, final String object, final Set<String> messageIds) {
		this.id = id;
		this.object = object;
		this.messageIds = messageIds;
	}

	@Override
	public String toString() {
		return object;
	}

	public String getId() {
		return id;
	}

	public String getObject() {
		return object;
	}

	public Set<String> getMessageIds() {
		return messageIds;
	}

}
