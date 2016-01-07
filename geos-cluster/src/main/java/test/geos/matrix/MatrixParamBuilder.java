package test.geos.matrix;

import java.io.File;

public class MatrixParamBuilder {
	private final String[] args;

	public MatrixParamBuilder(String... args) {
		this.args = args;
	}

	public MatrixParam build() throws MatrixParamBuilderException {
		if (args.length != 3) {
			throw new MatrixParamBuilderException("wrong number of arguments");
		}
		int x;
		try {
			x = Integer.valueOf(args[0]);
		} catch (final Exception e) {
			throw new MatrixParamBuilderException("invalid width", e);
		}
		final int y;
		try {
			y = Integer.valueOf(args[1]);
		} catch (final Exception e) {
			throw new MatrixParamBuilderException("invalid height", e);
		}
		final File file;
		try {
			file = new File(args[2]);
		} catch (final Exception e) {
			throw new MatrixParamBuilderException("invalid csv file path", e);
		}
		if (!file.exists()) {
			throw new MatrixParamBuilderException("invalid csv file path");
		}
		return new MatrixParam(x, y, file);
	}

}
