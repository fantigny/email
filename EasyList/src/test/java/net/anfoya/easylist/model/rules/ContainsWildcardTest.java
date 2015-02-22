package net.anfoya.easylist.model.rules;

import org.junit.Assert;
import org.junit.Test;

public class ContainsWildcardTest {

	@Test
	public void pattern() {
		ContainsHttpWildcard containsWildcard;
		
		containsWildcard = new ContainsHttpWildcard("||");
		Assert.assertTrue(containsWildcard.applies("http://"));
		Assert.assertTrue(containsWildcard.applies("https://"));

		Assert.assertFalse(containsWildcard.applies("XXX"));
		
		containsWildcard = new ContainsHttpWildcard("||cacheserve.*/promodisplay/");
		Assert.assertTrue(containsWildcard.applies("http://+++++.cacheserve.+/+/+/+/+/+/promodisplay/++++++++"));
	}
}
