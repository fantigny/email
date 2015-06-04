package net.anfoya.mail.model;

import net.anfoya.mail.service.Contact;

public class SimpleContact implements Contact {

	private final String email;
	private final String fullname;

	public SimpleContact(final String address, final String fullname) {
		this.email = address;
		this.fullname = fullname;
	}

	public String getEmail() {
		return email;
	}

	public String getFullname() {
		return fullname;
	}
}
