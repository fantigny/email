package net.anfoya.easylist.model.rules;

import org.junit.Assert;
import org.junit.Test;

public class ContainsWildcardTest {

	@Test
	public void pattern() {
		final ContainsWildcard rule = new ContainsWildcard("||cacheserve.*/promodisplay/");
		Assert.assertTrue(rule.applies("http://+++++.cacheserve.+/+/+/+/+/+/promodisplay/++++++++"));
	}
}
