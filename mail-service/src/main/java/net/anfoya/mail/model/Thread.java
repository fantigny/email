package net.anfoya.mail.model;

import java.util.List;


public class Thread {

	private final String id;
	private final String object;
	private final List<Message> messages;

	public Thread(final String id, final String object) {
		this(id, object, null);
	}

	public Thread(final String id, final String object, final List<Message> messages) {
		this.id = id;
		this.object = object;
		this.messages = messages;
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

	public List<Message> getMessages() {
		return messages;
	}

}
