package net.anfoya.mail.service;

@SuppressWarnings("serial")
public class MailServiceException extends Exception {

	public MailServiceException(final String msg, final Throwable t) {
		super(msg, t);
	}

}
