package net.anfoya.mail.gmail.model;

import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.service.Contact;

public class GmailContact extends SimpleContact implements Contact {

	public GmailContact(final String address, final String fullname) {
		super(address, fullname);
	}
}
