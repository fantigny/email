package net.anfoya.mail.service;

import net.anfoya.tag.service.TagException;

@SuppressWarnings("serial")
public abstract class MailException extends TagException {
	public MailException(final String msg, final Throwable e) {
		super(msg, e);
	}
}
