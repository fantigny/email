package net.anfoya.mail.gmail;

@SuppressWarnings("serial")
public class MessageException extends Exception {

	public MessageException(final String msg, final Throwable e) {
		super(msg, e);
	}
}
