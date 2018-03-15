package net.anfoya.java.undo;

@SuppressWarnings("serial")
public class UndoException extends Exception {

	public UndoException(final String msg, final Throwable e) {
		super("undoing \"" + msg + "\"", e);
	}
}
