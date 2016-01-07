package test.geos.cluster;

import java.io.PrintStream;
import java.util.Set;

import test.geos.geo.Geo;

public class Cluster {
	private final Set<Geo> geos;
	private final long size;
	private long sum;
	public Cluster(Set<Geo> geos) {
		this.geos = geos;

		size = geos.size();
		sum = -1;
	}
	public Set<Geo> getGeos() {
		return geos;
	}
	public void print(PrintStream stream) {
		for(final Geo g: geos) {
			g.print(stream);
		}
		stream.println();
	}
	public long getSize() {
		return size;
	}
	public long getSum() {
		if (sum == -1) {
			sum = geos.stream()
					.mapToLong(g -> g.getId())
					.sum();
		}
		return sum;
	}
}
