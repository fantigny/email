package net.anfoya.mail.gmail.service;

@SuppressWarnings("serial")
public class ContactException extends Exception {

	public ContactException(final String msg, final Throwable e) {
		super(msg, e);
	}

}
