package net.anfoya.mail.gmail;

@SuppressWarnings("serial")
public class ThreadException extends Exception {

	public ThreadException(final String msg, final Throwable e) {
		super(msg, e);
	}
}
