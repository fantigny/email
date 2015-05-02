package net.anfoya.mail.model;

import java.util.List;

public class Thread {

	private final String id;
	private final String object;
	private final List<String> mailIds;

	public Thread(final String id, final String object, final List<String> mailIds) {
		this.id = id;
		this.object = object;
		this.mailIds = mailIds;
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

	public List<String> getMailIds() {
		return mailIds;
	}

}
