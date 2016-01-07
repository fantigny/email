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
import org.junit.Before;
import org.junit.Test;

import test.geos.cluster.Cluster;
import test.geos.geo.Geo;
import test.geos.matrix.Matrix;
import test.geos.matrix.MatrixBuilder;
import test.geos.matrix.MatrixBuilderException;
import test.geos.matrix.MatrixParam;
import test.geos.matrix.MatrixParamBuilder;
import test.geos.matrix.MatrixParamBuilderException;

public class GeoClusterAnalyserTest {
	private static final String BIG_MATRIX_PATH = System.getProperty("java.io.tmpdir") + File.separator + "bigMatrix.csv";
	private static final String BIG_CLUSTER_PATH = System.getProperty("java.io.tmpdir") + File.separator + "bigCluster.csv";
	private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() { @Override public void write(int b) throws IOException {} };

	@Before
	public void init() throws IOException {
		final Random random = new Random(System.currentTimeMillis());
		final Calendar calendar = Calendar.getInstance();
		final String lineFormat = "%d, username%d, %s";
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		calendar.set(1900, 1, 1);
		int id = 0;
		try (FileWriter fileWriter = new FileWriter(BIG_MATRIX_PATH);
				BufferedWriter writer = new BufferedWriter(fileWriter)) {
			for(int i = 0; i < 10000; i++) {
				writer.write(String.format(lineFormat, id, id, dateFormat.format(calendar.getTime())));
				writer.newLine();
				id += random.nextInt(5);
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
		}

		calendar.set(1900, 1, 1);
		id = 0;
		try (FileWriter fileWriter = new FileWriter(BIG_CLUSTER_PATH);
				BufferedWriter writer = new BufferedWriter(fileWriter)) {
			for(int i = 0; i < 10000; i++) {
				writer.write(String.format(lineFormat, id, id, dateFormat.format(calendar.getTime())));
				writer.newLine();
				id += 1;
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
		}
	}

	@After
	public void clean() {
		new File(BIG_CLUSTER_PATH).delete();
		new File(BIG_MATRIX_PATH).delete();
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
	public void perfTest1() throws MatrixParamBuilderException, MatrixBuilderException {
		perfTest(10000, 10000, BIG_CLUSTER_PATH);
	}

	@Test
	public void perfTest2() throws MatrixParamBuilderException, MatrixBuilderException {
		perfTest(10000, 10000, BIG_MATRIX_PATH);
	}

	private void perfTest(int width, int height, String csvPath) throws MatrixParamBuilderException, MatrixBuilderException {
		final long start = System.currentTimeMillis();
		long inter = start;

		final MatrixParam param = new MatrixParamBuilder("" + width, "" + height, csvPath).build();
		System.out.println("MatrixParamBuilder: " + (System.currentTimeMillis()-inter));
		inter = System.currentTimeMillis();

		final Matrix<Geo> matrix = new MatrixBuilder(param).build();
		System.out.println("MatrixBuilder: " + (System.currentTimeMillis()-inter));
		inter = System.currentTimeMillis();

		final Cluster cluster = new GeoClusterAnalyser(matrix).getBiggestCluster();
		System.out.println("getBiggestCluster: " + (System.currentTimeMillis()-inter));

		cluster.print(new PrintStream(NULL_OUTPUT_STREAM));
		System.out.println("display: " + (System.currentTimeMillis()-inter));

		System.out.println("-----------------------");
		System.out.println("total: " + (System.currentTimeMillis()-start));
		System.out.println();
	}
}
