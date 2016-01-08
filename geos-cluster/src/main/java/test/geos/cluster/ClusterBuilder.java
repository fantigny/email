package test.geos.cluster;

import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import test.geos.geo.Geo;
import test.geos.matrix.Matrix;

public class ClusterBuilder {
	private final Matrix matrix;

	public ClusterBuilder(Matrix matrix) {
		this.matrix = matrix;
	}

	public Cluster buildFrom(Geo from) {
		final Set<Geo> path = new TreeSet<Geo>();
		final Stack<Geo> stack = new Stack<Geo>();
		stack.push(from);
		while(!stack.isEmpty()) {
			final Geo geo = stack.pop();
			path.add(geo);

			final long x = geo.getX();
			final long y = geo.getY();

			final Geo left = matrix.getAt(x - 1, y);
			if (left != null && !path.contains(left)) {
				stack.push(left);
			}
			final Geo down = matrix.getAt(x, y - 1);
			if (down != null && !path.contains(down)) {
				stack.push(down);
			}
			final Geo right = matrix.getAt(x + 1, y);
			if (right != null && !path.contains(right)) {
				stack.push(right);
			}
			final Geo up = matrix.getAt(x, y + 1);
			if (up != null && !path.contains(up)) {
				stack.push(up);
			}
		}
		return new Cluster(path);
	}

}
