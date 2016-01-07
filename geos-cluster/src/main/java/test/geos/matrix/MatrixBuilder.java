package test.geos.matrix;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import test.geos.geo.Geo;

public class MatrixBuilder {
	private final MatrixParam param;

	public MatrixBuilder(final MatrixParam param) {
		this.param = param;
	}

	public Matrix<Geo> build() throws MatrixBuilderException {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		final int width = param.getWidth();
		final int height = param.getHeight();
		final Geo[][] geos = new Geo[width][height];

		try (Scanner scanner = new Scanner(param.getCsv())) {
			scanner.useDelimiter("\\s*,\\s*|\r\n|\r|\n");
			long lineIndex = 1;
			while(scanner.hasNext()) {
				final long id;
				try {
					id = scanner.nextLong();
				} catch (final Exception e) {
					System.err.println("invalid id on line " + lineIndex);
					e.printStackTrace();
					continue;
				}
				final String name;
				try {
					name = scanner.next();
				} catch (final Exception e) {
					System.err.println("invalid id on line " + lineIndex);
					e.printStackTrace();
					continue;
				}
				final Date date;
				try {
					date = dateFormat.parse(scanner.next());
				} catch (final Exception e) {
					System.err.println("invalid id on line " + lineIndex);
					e.printStackTrace();
					continue;
				}

				final int x = (int) (id % width);
				final int y = height - 1 - (int) (id / width);
				geos[x][y] = new Geo(id, name, date);

				lineIndex++;
			}
		} catch (final Exception e) {
			throw new MatrixBuilderException(e);
		}
		return new Matrix<Geo>(geos);
	}
}
