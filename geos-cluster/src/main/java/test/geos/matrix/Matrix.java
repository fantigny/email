package test.geos.matrix;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import test.geos.geo.Geo;

public class Matrix {
	public static long toX(long width, long id) {
		return id % width;
	}
	public static long toY(long width, long height, long id) {
		return height - 1 - (int) (id / width);
	}

	private final long width;
	private final long height;
	private final Map<Long, Geo> data;
	public Matrix(long width, long height, Map<Long, Geo> data) {
		this.width = width;
		this.height = height;
		this.data = Collections.unmodifiableMap(data);
	}
	public long getWidth() {
		return width;
	}
	public long getHeight() {
		return height;
	}
	public Geo getAt(long x, long y) {
		return data.get(toId(x, y));
	}
	public boolean isEmpty(long x, long y) {
		return !data.containsKey(toId(x, y));
	}
	public Collection<Geo> getGeos() {
		return data.values();
	}
	private long toId(long x, long y) {
		return (height - 1 - y) * width + x;
	}
}