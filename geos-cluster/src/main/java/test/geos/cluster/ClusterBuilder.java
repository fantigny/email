package test.geos.cluster;

import java.util.HashSet;
import java.util.Set;

import test.geos.geo.Geo;
import test.geos.matrix.Matrix;

public class ClusterBuilder {
	enum Direction { TOP, RIGHT, BOTTOM, LEFT }

	private final Matrix<Geo> matrix;

	public ClusterBuilder(Matrix<Geo> matrix) {
		this.matrix = matrix;
	}

	public Cluster buildFrom(int x, int y) {
		final Set<Geo> path = new HashSet<Geo>();

		Geo geo = matrix.getAt(x, y);
		while (geo != null) {
			path.add(geo);
			geo = null;
			if (matrix.exists(x, y-1) && !path.contains(matrix.getAt(x, y-1))) {		// up
				geo = matrix.getAt(x, --y);
			} else if (matrix.exists(x+1, y) && !path.contains(matrix.getAt(x+1, y))) {	// right
				geo = matrix.getAt(++x, y);
			} else if (matrix.exists(x, y+1) && !path.contains(matrix.getAt(x, y+1))) {	// down
				geo = matrix.getAt(x, ++y);
			} else if (matrix.exists(x-1, y) && !path.contains(matrix.getAt(x-1, y))) {	// left
				geo = matrix.getAt(--x, y);
			}
		}
		return new Cluster(path);
	}
}
