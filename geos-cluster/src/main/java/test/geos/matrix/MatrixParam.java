package test.geos.matrix;

import java.io.File;

public class MatrixParam {
	private final int width;
	private final int height;
	private final File csv;
	public MatrixParam(int width, int height, File csv) {
		super();
		this.width = width;
		this.height = height;
		this.csv = csv;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public File getCsv() {
		return csv;
	}
}
