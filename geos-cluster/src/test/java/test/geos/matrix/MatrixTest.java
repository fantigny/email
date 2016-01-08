package test.geos.matrix;

import java.util.Collections;
import java.util.Random;

import org.junit.Test;

import junit.framework.Assert;
import test.geos.geo.Geo;

public class MatrixTest {

	@Test
	public void toX() {
		Assert.assertEquals(1, new Matrix(4, 7, null).toX(17));
	}

	@Test
	public void toY() {
		Assert.assertEquals(1, new Matrix(4, 7, null).toY(21));
	}

	@Test
	public void toId() {
		Assert.assertEquals(11, new Matrix(4, 7, null).toId(3, 4));
	}

	@Test
	public void reflexivity() {
		final long id = 1 + new Random(System.currentTimeMillis()).nextInt(50);

		final Matrix matrix = new Matrix(
				id * 2
				, id * 3
				, Collections.singletonMap(id, new Geo(id, null, null)));

		Assert.assertEquals(id, matrix.getAt(matrix.toX(id), matrix.toY(id)).getId());
	}
}
