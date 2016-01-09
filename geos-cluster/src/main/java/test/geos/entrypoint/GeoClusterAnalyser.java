package test.geos.entrypoint;

import java.io.PrintStream;
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
import test.geos.matrix.MatrixException;
import test.geos.matrix.MatrixParamBuilder;
import test.geos.matrix.MatrixParamException;

public class GeoClusterAnalyser {
	// short version
	public static void main(String... args) throws MatrixException, MatrixParamException {
		print(System.out,
				new GeoClusterAnalyser(
						new MatrixBuilder(
								new MatrixParamBuilder(
										args
										).build()
								).build()
						).getBiggestCluster());
	}

	private static void print(PrintStream stream, Cluster cluster) {
		if (cluster == null) {
			stream.println("no cluster found");
		} else {
			stream.println("The Geos in the largest cluster of occupied Geos for this GeoBlock are:");
			Cluster.print(stream, cluster);
		}
		stream.println();
	}

	/****************/
	/** END STATIC **/
	/****************/

	private final Matrix matrix;

	public GeoClusterAnalyser(Matrix matrix) {
		this.matrix = matrix;
	}

	public List<Cluster> getClusters() {
		final List<Cluster> clusters = new ArrayList<Cluster>();

		final ClusterBuilder builder = new ClusterBuilder(matrix);
		final Set<Geo> clusteredGeos = new HashSet<Geo>();
		matrix.getGeos().forEach(g -> {
			if (!clusteredGeos.contains(g)) {
				final Cluster cluster = builder.buildFrom(g);
				clusters.add(cluster);
				clusteredGeos.addAll(cluster.getGeos());
			}
		});

		return clusters;
	}

	public Cluster getBiggestCluster() {
		final List<Cluster> clusters = getClusters();
		final long maxSize = clusters
				.parallelStream()
				.mapToLong(Cluster::getSize)
				.max()
				.orElse(0);

		final Cluster biggest;
		if (maxSize == 0) {
			biggest = null;
		} else {
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
