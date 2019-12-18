package net.anfoya.mail.gmail;

import net.anfoya.mail.service.MailException;

@SuppressWarnings("serial")
public class GMailException extends MailException {
	public GMailException(final String msg, final Throwable e) {
		super(msg, e);
	}

	public GMailException(String msg) {
		super(msg);
	}
}
