package test.geos.matrix;

import java.io.File;

public class MatrixParamBuilder {
	private final String[] args;

	public MatrixParamBuilder(String... args) {
		this.args = args;
	}

	public MatrixParam build() throws MatrixParamException {
		if (args.length != 3) {
			throw new MatrixParamException("wrong number of arguments");
		}
		int x;
		try {
			x = Integer.valueOf(args[0]);
		} catch (final Exception e) {
			throw new MatrixParamException("invalid width (" + args[0] + ")", e);
		}
		if (x < 1) {
			throw new MatrixParamException("invalid width (" + args[0] + ")");
		}
		final int y;
		try {
			y = Integer.valueOf(args[1]);
		} catch (final Exception e) {
			throw new MatrixParamException("invalid height (" + args[1] + ")", e);
		}
		if (y < 1) {
			throw new MatrixParamException("invalid height (" + args[1] + ")");
		}
		final File file;
		try {
			file = new File(args[2]);
		} catch (final Exception e) {
			throw new MatrixParamException("invalid csv file path", e);
		}
		if (!file.exists()) {
			throw new MatrixParamException("invalid csv file path");
		}
		return new MatrixParam(x, y, file);
	}

}
