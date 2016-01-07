package test.geos.matrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import test.geos.geo.Geo;
import test.geos.geo.GeoBuilder;
import test.geos.geo.GeoException;

public class MatrixBuilder {
	private final MatrixParam param;

	public MatrixBuilder(final MatrixParam param) {
		this.param = param;
	}

	public Matrix<Geo> build() throws MatrixException {
		final GeoBuilder builder = new GeoBuilder();

		final int width = param.getWidth();
		final int height = param.getHeight();
		final Geo[][] geos = new Geo[width][height];
		final long maxId = width * height - 1;

		try (FileReader fileReader = new FileReader(param.getCsv());
				BufferedReader reader = new BufferedReader(fileReader)) {
			long lineIndex = 0;
			String line;
			while((line = reader.readLine()) != null) {
				lineIndex++;

				Geo geo;
				try {
					geo = builder.build(line);
				} catch (final GeoException e) {
					System.err.println(e.getMessage() + " on line " + lineIndex);
					continue;
				}

				final long id = geo.getId();
				if (id > maxId) {
					System.err.println("id too large on line " + lineIndex);
					continue;
				}

				final int x = (int) (id % width);
				final int y = height - 1 - (int) (id / width);
				if (geos[x][y] != null) {
					System.err.println("duplicate id on line " + lineIndex);
					continue;
				}

				geos[x][y] = geo;
			}
		} catch (final IOException e) {
			throw new MatrixException("parsing matrix", e);
		}

		return new Matrix<Geo>(geos);
	}
}
