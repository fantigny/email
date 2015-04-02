package net.anfoya.java.net.filtered.easylist;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

public class EasyListRuleSetTest {

	@Test
	@Ignore
	public void filter() {
		final String url = "http://bs.serving-sys.com/BurstingPipe/adServer.bs?cn=tf&c=19&mc=imp&pli=12880179&PluID=0&ord=5347343&rtu=-1";

		final EasyListRuleSet ruleSet = new EasyListRuleSet(false);
		ruleSet.loadInternet();
		final boolean match = ruleSet.matchesExclusion(url);
		Assert.assertTrue(match);
	}
}