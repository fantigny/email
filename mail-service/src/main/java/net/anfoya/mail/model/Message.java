package net.anfoya.mail.model;

public class Message {

	private final String id;
	private final String snippet;

	public Message(final String id, final String snippet) {
		this.id = id;
		this.snippet = snippet;
	}

	public String getId() {
		return id;
	}

	public String getSnippet() {
		return snippet;
	}

}
