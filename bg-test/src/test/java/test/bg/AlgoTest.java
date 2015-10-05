package test.bg;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class AlgoTest {

	private Algo algo;

	@Before
	public void init() {
		algo = new Algo();
	}

	@Test
	public void nullSource() {
		final int pos = algo.getStartPos(null, new int[] { 1 });
		Assert.assertEquals(-1, pos);
	}

	@Test
	public void emptySource() {
		final int pos = algo.getStartPos(new int[0], new int[] { 1 });
		Assert.assertEquals(-1, pos);
	}

	@Test
	public void nullSearch() {
		final int pos = algo.getStartPos(new int[] { 1 }, null);
		Assert.assertEquals(-1, pos);
	}

	@Test
	public void emptySearch() {
		final int pos = algo.getStartPos(new int[] { 1 }, new int[0]);
		Assert.assertEquals(-1, pos);
	}

	@Test
	public void first() {
		final int pos = algo.getStartPos(new int[] { 1, 2, 3 }, new int[] { 1, 10, 100 });
		Assert.assertEquals(0, pos);
	}

	@Test
	public void last() {
		final int pos = algo.getStartPos(new int[] { 3, 2, 1 }, new int[] { 1, 10, 100 });
		Assert.assertEquals(2, pos);
	}

	@Test
	public void any() {
		final int pos = algo.getStartPos(new int[] { 2, 3, 4, 5 }, new int[] { 4, 5 });
		Assert.assertEquals(2, pos);
	}

	@Test
	public void none() {
		final int pos = algo.getStartPos(new int[] { 1, 2, 3 }, new int[] { 6, 6, 6 });
		Assert.assertEquals(-1, pos);
	}

	@Test
	public void repeat() {
		final int pos = algo.getStartPos(new int[] { 1, 1, 1, 1 }, new int[] { 1, 1, 1, 1 });
		Assert.assertEquals(0, pos);
	}
}
