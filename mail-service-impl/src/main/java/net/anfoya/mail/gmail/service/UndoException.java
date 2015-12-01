package net.anfoya.mail.gmail.service;

@SuppressWarnings("serial")
public class UndoException extends Exception {

	public UndoException(final String msg, final Throwable e) {
		super(msg, e);
	}
}
