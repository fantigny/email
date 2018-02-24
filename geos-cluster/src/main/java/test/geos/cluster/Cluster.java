package test.geos.cluster;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Set;

import test.geos.geo.Geo;

public class Cluster {
	public static void print(PrintStream stream, Cluster cluster) {
		cluster.geos.forEach(g -> Geo.print(stream, g));
	}

	private final Set<Geo> geos;
	private long sum;
	public Cluster(Set<Geo> geos) {
		this.geos = geos;
		sum = -1; // lazy
	}
	public Set<Geo> getGeos() {
		return Collections.unmodifiableSet(geos);
	}
	public long getSize() {
		return geos.size();
	}
	public long getSum() {
		if (sum == -1) {
			sum = geos
					.stream()
					.mapToLong(g -> g.getId())
					.sum();
		}
		return sum;
	}
}
