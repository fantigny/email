package test.geos.matrix;

import java.io.File;

public class MatrixParam {
	private final long width;
	private final long height;
	private final File csv;
	public MatrixParam(long width, long height, File csv) {
		super();
		this.width = width;
		this.height = height;
		this.csv = csv;
	}
	public long getWidth() {
		return width;
	}
	public long getHeight() {
		return height;
	}
	public File getCsv() {
		return csv;
	}
}
