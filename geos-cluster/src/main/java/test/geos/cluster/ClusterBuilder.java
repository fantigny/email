package test.geos.cluster;

import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import test.geos.geo.Geo;
import test.geos.matrix.Matrix;

public class ClusterBuilder {
	enum Direction { TOP, RIGHT, BOTTOM, LEFT }
	private class Coordinate {
		private final int x;
		private final int y;
		public Coordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	private final Matrix<Geo> matrix;

	public ClusterBuilder(Matrix<Geo> matrix) {
		this.matrix = matrix;
	}

	public Cluster buildFrom(final int x, final int y) {
		final Set<Geo> path = new TreeSet<Geo>();
		final Stack<Coordinate> stack = new Stack<Coordinate>();
		stack.push(new Coordinate(x, y));
		while(!stack.isEmpty()) {
			final Coordinate c = stack.pop();
			path.add(matrix.getAt(c.x, c.y));

			final int up = c.y-1, down = c.y+1, right=c.x-1, left=c.x+1;
			if (matrix.exists(right, c.y) && !path.contains(matrix.getAt(right, c.y))) {
				stack.push(new Coordinate(right, c.y));
			}
			if (matrix.exists(c.x, up) && !path.contains(matrix.getAt(c.x, up))) {
				stack.push(new Coordinate(c.x, up));
			}
			if (matrix.exists(c.x, down) && !path.contains(matrix.getAt(c.x, down))) {
				stack.push(new Coordinate(c.x, down));
			}
			if (matrix.exists(left, c.y) && !path.contains(matrix.getAt(left, c.y))) {
				stack.push(new Coordinate(left, c.y));
			}
		}
		return new Cluster(path);
	}
}
