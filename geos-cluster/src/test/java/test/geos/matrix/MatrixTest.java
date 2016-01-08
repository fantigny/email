package test.geos.matrix;

import java.util.Collections;
import java.util.Date;

import org.junit.Test;

import junit.framework.Assert;
import test.geos.geo.Geo;

public class MatrixTest {

	@Test
	public void toX() {
		Assert.assertEquals(1, Matrix.toX(4, 17));
	}

	@Test
	public void toY() {
		Assert.assertEquals(1, Matrix.toY(4, 7, 21));
	}

	@Test
	public void coordinate() {
		final long width = 7, height = 4, id = 22;
		final Matrix matrix = new Matrix(
				width
				, height
				, Collections.singletonMap(
						id
						, new Geo(id
								, "" + id
								, new Date()
								, Matrix.toX(width, id)
								, Matrix.toY(width, height, id)
								)));

		Assert.assertEquals(id, matrix.getAt(1, 0).getId());
	}
}
