package test.geos.entrypoint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import test.geos.cluster.Cluster;
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
	public void emptyFile() {
		GeoClusterAnalyser.main(
				"2"
				, "2"
				, "src/test/resources/empty.csv");
	}

	@Test
	public void tCluster() {
		GeoClusterAnalyser.main(
				"3"
				, "3"
				, "src/test/resources/tCluster.csv");
	}

	@Test
	public void duplicateIdFile() {
		GeoClusterAnalyser.main(
				"2"
				, "2"
				, "src/test/resources/empty.csv");
	}

	@Test
	public void badMatrixSize() {
		GeoClusterAnalyser.main(
				"3"
				, "3"
				, "src/test/resources/GeoBlockExample.csv");
	}

	@Test
	public void badWidth() {
		GeoClusterAnalyser.main(
				"0"
				, "1"
				, null);
	}

	@Test
	public void badHeight() {
		GeoClusterAnalyser.main(
				"1"
				, "99999999999999999999999999999999999999999999999999999999999"
				, null);
	}

	@Test
	public void badFile() {
		GeoClusterAnalyser.main(
				"2"
				, "2"
				, "src/test/resources/bad.csv");
	}

	@Test
	public void useCase1() {
		GeoClusterAnalyser.main(
				"4"
				, "7"
				, "src/test/resources/GeoBlockExample.csv");
	}

	@Test
	public void useCase2() {
		GeoClusterAnalyser.main(
				"7"
				, "4"
				, "src/test/resources/GeoBlockExample.csv");
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

		cluster.print(new PrintStream(NULL_OUTPUT_STREAM));
		System.out.println("Cluster::print " + (System.currentTimeMillis()-inter) + "ms");

		System.out.println();

		return cluster;
	}
}
