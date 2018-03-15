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
		long width;
		try {
			width = Long.valueOf(args[0]);
		} catch (final Exception e) {
			throw new MatrixParamException("invalid width (" + args[0] + ")", e);
		}
		if (width < 1) {
			throw new MatrixParamException("invalid width (" + args[0] + "), should be greater than zero");
		}
		final long height;
		try {
			height = Long.valueOf(args[1]);
		} catch (final Exception e) {
			throw new MatrixParamException("invalid height (" + args[1] + ")", e);
		}
		if (height < 1) {
			throw new MatrixParamException("invalid height (" + args[1] + ") should be greater than zero");
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
		return new MatrixParam(width, height, file);
	}

}
