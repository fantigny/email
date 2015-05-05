package net.anfoya.mail.model;

public class Message {

	private final String id;
	private final String body;

	public Message(final String id, final String body) {
		this.id = id;
		this.body = body;
	}

	public String getId() {
		return id;
	}

	public String getBody() {
		return body;
	}

}
