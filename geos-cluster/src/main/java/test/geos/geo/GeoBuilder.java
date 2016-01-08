package test.geos.geo;

import java.text.SimpleDateFormat;
import java.util.Date;

import test.geos.matrix.Matrix;

public class GeoBuilder {

	private final long width;
	private final long height;
	private final SimpleDateFormat dateFormat;
	private final long maxId;

	public GeoBuilder(long matrixWidth, long matrixHeight) {
		width = matrixWidth;
		height = matrixHeight;
		maxId = width * height - 1;
		dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	}

	public Geo build(String line) throws GeoException {
		final String[] geoData = line.split("\\s*,\\s*");
		if (geoData.length < 3) {
			throw new GeoException("missing data");
		}
		if (geoData.length > 3) {
			throw new GeoException("too much data");
		}
		final long id;
		try {
			id = Long.valueOf(geoData[0]);
		} catch (final Exception e) {
			throw new GeoException("invalid id (" + geoData[0] + ")", e);
		}
		if (id < 0) {
			throw new GeoException("invalid id (" + id + ")");
		}
		if (id > maxId) {
			throw new GeoException("id (" + id + ") doesn't fit in matrix");
		}
		final String name = geoData[1];
		final Date date;
		try {
			date = dateFormat.parse(geoData[2]);
		} catch (final Exception e) {
			throw new GeoException("invalid date (" + geoData[2] + ")", e);
		}

		return new Geo(id, name, date, Matrix.toX(width, id), Matrix.toY(width, height, id));
	}
}
