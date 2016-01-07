package test.geos.entrypoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import test.geos.cluster.Cluster;
import test.geos.cluster.ClusterBuilder;
import test.geos.geo.Geo;
import test.geos.matrix.Matrix;
import test.geos.matrix.MatrixBuilder;
import test.geos.matrix.MatrixBuilderException;
import test.geos.matrix.MatrixParam;
import test.geos.matrix.MatrixParamBuilder;
import test.geos.matrix.MatrixParamBuilderException;

public class GeoClusterAnalyser {

	public static void main(String... args) {
		MatrixParam param = null;
		try {
			param = new MatrixParamBuilder(args).build();
		} catch (final MatrixParamBuilderException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		Matrix<Geo> matrix = null;
		try {
			matrix = new MatrixBuilder(param).build();
		} catch (final MatrixBuilderException e) {
			e.printStackTrace();
			System.exit(-2);
		}

		final Cluster cluster = new GeoClusterAnalyser(matrix).getBiggestCluster();

		if (cluster == null) {
			System.out.println("no cluster found");
		} else {
			System.out.println("The Geos in the largest cluster of occupied Geos for this GeoBlock are:");
			cluster.print(System.out);
		}
	}

	private final Matrix<Geo> matrix;

	public GeoClusterAnalyser(Matrix<Geo> matrix) {
		this.matrix = matrix;
	}
	public List<Cluster> getClusters() {
		final List<Cluster> clusters = new ArrayList<Cluster>();

		final ClusterBuilder builder = new ClusterBuilder(matrix);
		final Set<Geo> clusteredGeos = new HashSet<Geo>();
		final int width = matrix.getWidth(), height = matrix.getHeight();
		for(int x=0; x < width; x++) {
			for(int y=0; y < height; y++) {
				if (!matrix.isNull(x, y) && !clusteredGeos.contains(matrix.getAt(x, y))) {
					final Cluster cluster = builder.buildFrom(x, y);
					clusters.add(cluster);
					clusteredGeos.addAll(cluster.getGeos());
				}
			}
		}
		return clusters;
	}

	public Cluster getBiggestCluster() {
		Cluster biggest = null;

		final List<Cluster> clusters = getClusters();
		final long maxSize = clusters
				.parallelStream()
				.mapToLong(Cluster::getSize)
				.max()
				.orElse(0);
		if (maxSize > 0) {
			final long minSum = clusters
					.parallelStream()
					.filter(c -> c.getSize() == maxSize)
					.mapToLong(Cluster::getSum)
					.min()
					.orElse(0);
			biggest = clusters
					.parallelStream()
					.filter(c -> c.getSum() == minSum)
					.collect(Collectors.toList())
					.get(0);
		}

		return biggest;
	}
}
