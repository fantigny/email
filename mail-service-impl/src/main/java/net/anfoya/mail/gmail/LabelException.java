package net.anfoya.mail.gmail;

@SuppressWarnings("serial")
public class LabelException extends Exception {

	public LabelException(final String msg, final Throwable e) {
		super(msg, e);
	}
}
