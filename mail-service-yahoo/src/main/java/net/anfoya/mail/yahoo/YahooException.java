package net.anfoya.mail.yahoo;

import net.anfoya.mail.service.MailException;

@SuppressWarnings("serial")
public class YahooException extends MailException {
	public YahooException(final String msg, final Throwable e) {
		super(msg, e);
	}

	public YahooException(String msg) {
		super(msg);
	}
}
