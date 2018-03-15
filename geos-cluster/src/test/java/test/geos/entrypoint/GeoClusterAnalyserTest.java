package test.geos.entrypoint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import test.geos.cluster.Cluster;
import test.geos.geo.Geo;
import test.geos.matrix.Matrix;
import test.geos.matrix.MatrixBuilder;
import test.geos.matrix.MatrixException;
import test.geos.matrix.MatrixParam;
import test.geos.matrix.MatrixParamBuilder;
import test.geos.matrix.MatrixParamException;

public class GeoClusterAnalyserTest {
	private static final String TEMP_FOLDER = System.getProperty("java.io.tmpdir") + File.separator;
	private static final String TEN_THOUSAND_GEOS_PATH = TEMP_FOLDER + "10,000-geos.csv";
	private static final String TEN_THOUSAND_GEOS_CLUSTER_PATH = TEMP_FOLDER + "10,000-geos-cluster.csv";
	private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() { @Override public void write(int b) throws IOException {} };

	@Before
	public void init() throws IOException {
		final Random random = new Random(System.currentTimeMillis());
		final Calendar calendar = Calendar.getInstance();
		final String lineFormat = "%d, name%d, %s";
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		calendar.set(1, 1, 1);
		int id1 = 0;
		int id2 = 0;
		try (BufferedWriter writer1 = new BufferedWriter(new FileWriter(TEN_THOUSAND_GEOS_PATH));
				BufferedWriter writer2 = new BufferedWriter(new FileWriter(TEN_THOUSAND_GEOS_CLUSTER_PATH))) {
			for(int i = 0; i < 10000; i++) {
				final String d = dateFormat.format(calendar.getTime());

				writer1.write(String.format(lineFormat, id1, id1, d));
				writer1.newLine();
				id1 += 1 + random.nextInt(3);

				writer2.write(String.format(lineFormat, id2, id2, d));
				writer2.newLine();
				id2 += 1;

				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
		}
	}

	@After
	public void clean() {
		new File(TEN_THOUSAND_GEOS_CLUSTER_PATH).delete();
		new File(TEN_THOUSAND_GEOS_PATH).delete();
	}

	@Test
	public void useCase1() throws MatrixException, MatrixParamException {
		final MatrixParam param = new MatrixParamBuilder("4", "7", "src/test/resources/GeoBlockExample.csv").build();
		final Cluster cluster = new GeoClusterAnalyser(new MatrixBuilder(param).build()).getBiggestCluster();
		Assert.assertEquals(4, cluster.getSize());

		final Iterator<Geo> iterator = cluster.getGeos().iterator();
		Assert.assertEquals(13, iterator.next().getId());
		Assert.assertEquals(17, iterator.next().getId());
		Assert.assertEquals(21, iterator.next().getId());
		Assert.assertEquals(22, iterator.next().getId());
	}

	@Test
	public void useCase2() throws MatrixException, MatrixParamException {
		final MatrixParam param = new MatrixParamBuilder("7", "4", "src/test/resources/GeoBlockExample.csv").build();
		final Cluster cluster = new GeoClusterAnalyser(new MatrixBuilder(param).build()).getBiggestCluster();
		Assert.assertEquals(5, cluster.getSize());

		final Iterator<Geo> iterator = cluster.getGeos().iterator();
		Assert.assertEquals(4, iterator.next().getId());
		Assert.assertEquals(5, iterator.next().getId());
		Assert.assertEquals(6, iterator.next().getId());
		Assert.assertEquals(11, iterator.next().getId());
		Assert.assertEquals(13, iterator.next().getId());
	}

	@Test
	public void emptyFile() throws MatrixException, MatrixParamException {
		final MatrixParam param = new MatrixParamBuilder("7", "4", "src/test/resources/empty.csv").build();
		final Cluster cluster = new GeoClusterAnalyser(new MatrixBuilder(param).build()).getBiggestCluster();
		Assert.assertNull(cluster);
	}

	@Test
	public void plusCluster() throws MatrixException, MatrixParamException {
		// 3 x 3 matrix with + shape cluster		. | x | .
		//											x | x | x
		//											. | x | .
		final MatrixParam param = new MatrixParamBuilder("3", "3", "src/test/resources/plusCluster.csv").build();
		final Cluster cluster = new GeoClusterAnalyser(new MatrixBuilder(param).build()).getBiggestCluster();
		Assert.assertEquals(5, cluster.getSize());

		final Iterator<Geo> iterator = cluster.getGeos().iterator();
		Assert.assertEquals(1, iterator.next().getId());
		Assert.assertEquals(3, iterator.next().getId());
		Assert.assertEquals(4, iterator.next().getId());
		Assert.assertEquals(5, iterator.next().getId());
		Assert.assertEquals(7, iterator.next().getId());
	}

	@Test
	public void duplicateIdFile() throws MatrixException, MatrixParamException {
		final MatrixParam param = new MatrixParamBuilder("3", "3", "src/test/resources/duplicateId.csv").build();
		final Cluster cluster = new GeoClusterAnalyser(new MatrixBuilder(param).build()).getBiggestCluster();
		Assert.assertEquals(1, cluster.getSize());

		final Iterator<Geo> iterator = cluster.getGeos().iterator();
		Assert.assertEquals(1, iterator.next().getId());
	}

	@Test
	public void badIds() throws MatrixException, MatrixParamException {
		final MatrixParam param = new MatrixParamBuilder("3", "3", "src/test/resources/GeoBlockExample.csv").build();
		final Cluster cluster = new GeoClusterAnalyser(new MatrixBuilder(param).build()).getBiggestCluster();
		Assert.assertEquals(3, cluster.getSize());

		final Iterator<Geo> iterator = cluster.getGeos().iterator();
		Assert.assertEquals(4, iterator.next().getId());
		Assert.assertEquals(5, iterator.next().getId());
		Assert.assertEquals(6, iterator.next().getId());
	}

	@Test(expected=MatrixParamException.class)
	public void badWidth() throws MatrixException, MatrixParamException {
		GeoClusterAnalyser.main(
				"0"
				, "1"
				, null);
	}

	@Test(expected=MatrixParamException.class)
	public void badHeight() throws MatrixException, MatrixParamException {
		GeoClusterAnalyser.main(
				"1"
				, "99999999999999999999999999999999999999999999999999999999999"
				, null);
	}

	@Test
	public void badFile() throws MatrixException, MatrixParamException {
		final MatrixParam param = new MatrixParamBuilder("2", "2", "src/test/resources/bad.csv").build();
		final Cluster cluster = new GeoClusterAnalyser(new MatrixBuilder(param).build()).getBiggestCluster();
		Assert.assertEquals(1, cluster.getSize());

		final Iterator<Geo> iterator = cluster.getGeos().iterator();
		Assert.assertEquals(1, iterator.next().getId());
	}

	@Test
	public void sameSize() throws MatrixParamException, MatrixException {
		final MatrixParam param = new MatrixParamBuilder("4", "7", "src/test/resources/sameSize.csv").build();
		final Cluster cluster = new GeoClusterAnalyser(new MatrixBuilder(param).build()).getBiggestCluster();
		Assert.assertEquals(3, cluster.getSize());

		final Iterator<Geo> iterator = cluster.getGeos().iterator();
		Assert.assertEquals(4, iterator.next().getId());
		Assert.assertEquals(5, iterator.next().getId());
		Assert.assertEquals(6, iterator.next().getId());
	}

	@Test
	public void perfTest1() throws MatrixParamException, MatrixException {
		final Cluster cluster = perfTest(10000, 10000, TEN_THOUSAND_GEOS_CLUSTER_PATH);
		Assert.assertEquals(10000, cluster.getSize());
	}

	@Test
	public void perfTest2() throws MatrixParamException, MatrixException {
		perfTest(10000, 10000, TEN_THOUSAND_GEOS_PATH);
	}

	private Cluster perfTest(int width, int height, String csvPath) throws MatrixParamException, MatrixException {
		final MatrixParam param = new MatrixParamBuilder("" + width, "" + height, csvPath).build();

		long inter = System.currentTimeMillis();
		final Matrix matrix = new MatrixBuilder(param).build();
		System.out.println("MatrixBuilder::build " + (System.currentTimeMillis()-inter) + "ms");

		inter = System.currentTimeMillis();
		final Cluster cluster = new GeoClusterAnalyser(matrix).getBiggestCluster();
		System.out.println("GeoClusterAnalyser::getBiggestCluster " + (System.currentTimeMillis()-inter) + "ms");

		inter = System.currentTimeMillis();
		Cluster.print(new PrintStream(NULL_OUTPUT_STREAM), cluster);
		System.out.println("Cluster::print " + (System.currentTimeMillis()-inter) + "ms");

		System.out.println();

		return cluster;
	}
}
