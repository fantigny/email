package net.anfoya.easylist.model.rules;

import org.junit.Assert;
import org.junit.Test;

public class ContainsTest {
//	private static final String REGEX_SEP = "[^A-Za-z0-9_%.-]";

	@Test
	public void separator() {
		Contains contains;

		contains = new Contains("^");
		Assert.assertFalse(contains.applies("A"));
		Assert.assertFalse(contains.applies("b"));
		Assert.assertFalse(contains.applies("_"));
		Assert.assertFalse(contains.applies("%"));
		Assert.assertFalse(contains.applies("."));
		Assert.assertFalse(contains.applies("-"));

		Assert.assertTrue(contains.applies("/"));
		Assert.assertTrue(contains.applies(":"));
		Assert.assertTrue(contains.applies("?"));

		contains = new Contains("http^^^www.test.com^80^");
		Assert.assertTrue(contains.applies("http://www.test.com:80/"));
	}

	@Test
	public void wildcard() {
		Contains contains;

		contains = new Contains("*");
		Assert.assertTrue(contains.applies("Ab_%.-"));

		contains = new Contains("http*www.test.com*/");
		Assert.assertTrue(contains.applies("http://www.test.com:80/"));
	}

	@Test
	public void mixed() {
		Contains contains;

		contains = new Contains("^*^");
		Assert.assertTrue(contains.applies("/Ab_%.-/"));

		contains = new Contains("http^^^www.test.*^80^");
		Assert.assertTrue(contains.applies("http://www.test.XXX:80/"));
		Assert.assertTrue(contains.applies("http://www.test.YYY:80/"));

		contains = new Contains("/cdn-cgi/pe/bag?r[]=*cpalead.com");
		Assert.assertTrue(contains.applies("XXX/cdn-cgi/pe/bag?r[]=YYYcpalead.comZZZ"));
	}
}
