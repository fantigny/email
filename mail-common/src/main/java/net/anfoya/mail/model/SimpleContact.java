package net.anfoya.mail.model;

public class SimpleContact implements Contact {

	private final String email;
	private final String fullname;

	public SimpleContact(final String address, final String fullname) {
		this.email = address;
		this.fullname = fullname;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public String getFullname() {
		return fullname;
	}
}
