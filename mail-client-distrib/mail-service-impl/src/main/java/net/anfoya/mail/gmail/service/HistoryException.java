package net.anfoya.mail.gmail.service;

@SuppressWarnings("serial")
public class HistoryException extends Exception {

	public HistoryException(final String msg, final Throwable e) {
		super(msg, e);
	}
}
