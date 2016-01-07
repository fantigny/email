package test.geos.matrix;

@SuppressWarnings("serial")
public class MatrixException extends Exception {

	public MatrixException(Throwable t) {
		super(t);
	}

	public MatrixException(String msg) {
		super(msg);
	}

	public MatrixException(String msg, Throwable t) {
		super(msg, t);
	}

}
