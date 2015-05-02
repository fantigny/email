package net.anfoya.mail.model;

public class Thread {

	private final String id;

	public Thread(final String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}

	public String getId() {
		return id;
	}

}
