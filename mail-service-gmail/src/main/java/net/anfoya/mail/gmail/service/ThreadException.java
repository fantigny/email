package net.anfoya.mail.gmail.service;

@SuppressWarnings("serial")
public class ThreadException extends Exception {

	public ThreadException(final String msg, final Throwable e) {
		super(msg, e);
	}

	public ThreadException(final String msg) {
		super(msg);
	}
}
