package test.geos.matrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import test.geos.geo.Geo;
import test.geos.geo.GeoBuilder;
import test.geos.geo.GeoException;

public class MatrixBuilder {
	private final MatrixParam param;

	public MatrixBuilder(final MatrixParam param) {
		this.param = param;
	}

	public Matrix build() throws MatrixException {
		final long width = param.getWidth();
		final long height = param.getHeight();
		final GeoBuilder builder = new GeoBuilder(width, height);
		final Map<Long, Geo> data = new HashMap<Long, Geo>();

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
					geo = null;
					System.err.println(e.getMessage() + " on line " + lineIndex);
				}

				if (geo != null) {
					geo = data.put(geo.getId(), geo);
				}

				if (geo != null) {
					System.err.println("duplicate id (" + geo.getId() + ") on line " + lineIndex);
				}
			}
		} catch (final IOException e) {
			throw new MatrixException("parsing matrix", e);
		}

		return new Matrix(width, height, data);
	}
}
