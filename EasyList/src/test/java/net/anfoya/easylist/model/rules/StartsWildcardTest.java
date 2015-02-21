package net.anfoya.easylist.model.rules;

import org.junit.Assert;
import org.junit.Test;

public class StartsWildcardTest {

	@Test
	public void pattern() {

		final StartsWildcard rule = new StartsWildcard("||cacheserve.*/promodisplay/");
		Assert.assertTrue(rule.applies("http://+++++.cacheserve.+/+/+/+/+/+/promodisplay/++++++++"));
	}
}
