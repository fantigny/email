package net.anfoya.tag.service;


@SuppressWarnings("serial")
public abstract class TagException extends Exception {
	public TagException(final String msg, final Throwable t) {
		super(msg, t);
	}
}
