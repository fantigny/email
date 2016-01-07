package test.geos.matrix;

public class Matrix<T> {
	public final T[][] tArray;
	public final int width;
	public final int height;
	public Matrix(T[][] tArray) {
		this.tArray = tArray;
		width = tArray.length;
		height = tArray[0].length;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public T getAt(int x, int y) {
		return tArray[x][y];
	}
	public boolean isNull(int x, int y) {
		return getAt(x, y) == null;
	}
	public boolean exists(int x, int y) {
		return x >= 0
				&& x < width
				&& y >= 0
				&& y < height
				&& getAt(x, y) != null;
	}
}
