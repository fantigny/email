package net.anfoya.mail.gmail.service;

@SuppressWarnings("serial")
public class LabelException extends Exception {

	public LabelException(final String msg, final Throwable e) {
		super(msg, e);
	}
}
