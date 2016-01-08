package test.geos.cluster;

import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import test.geos.geo.Geo;
import test.geos.matrix.Matrix;

public class ClusterBuilder {
	private class Coordinate {
		protected final long x;
		protected final long y;
		protected Coordinate(long x, long y) {
			this.x = x;
			this.y = y;
		}
		protected Coordinate up() 		{ return new Coordinate(x, y + 1); }
		protected Coordinate down() 	{ return new Coordinate(x, y - 1); }
		protected Coordinate left()		{ return new Coordinate(x - 1, y); }
		protected Coordinate right()	{ return new Coordinate(x + 1, y); }
	}

	private final Matrix matrix;

	public ClusterBuilder(Matrix matrix) {
		this.matrix = matrix;
	}

	public Cluster buildFrom(Geo from) {
		final Set<Geo> path = new TreeSet<Geo>();

		final Stack<Coordinate> stack = new Stack<Coordinate>();
		stack.push(new Coordinate(matrix.toX(from.getId()), matrix.toY(from.getId())));

		while(!stack.isEmpty()) {
			final Coordinate c = stack.pop();
			path.add(matrix.getAt(c.x, c.y));

			final Geo left = matrix.getAt(c.left().x, c.left().y);
			if (left != null && !path.contains(left)) {
				stack.push(c.left());
			}
			final Geo down = matrix.getAt(c.down().x, c.down().y);
			if (down != null && !path.contains(down)) {
				stack.push(c.down());
			}
			final Geo right = matrix.getAt(c.right().x, c.right().y);
			if (right != null && !path.contains(right)) {
				stack.push(c.right());
			}
			final Geo up = matrix.getAt(c.up().x, c.up().y);
			if (up != null && !path.contains(up)) {
				stack.push(c.up());
			}
		}
		return new Cluster(path);
	}

}
