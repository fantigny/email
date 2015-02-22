package net.anfoya.easylist.loader;

import net.anfoya.easylist.model.Rule2;
import net.anfoya.easylist.model.rules.Contains;
import net.anfoya.easylist.model.rules.ContainsHttpWildcard;
import net.anfoya.easylist.model.rules.EmptyRule;

import org.junit.Assert;
import org.junit.Test;

public class ParserTest {

	@Test
	public void pattern() {
		Rule2 rule;
		final Parser parser = new Parser();

		rule = parser.parse("imdb.com###top_rhs_1_wrapper");
		Assert.assertEquals(rule.getClass(), EmptyRule.class);

		rule = parser.parse("||cacheserve.*/promodisplay/");
		Assert.assertEquals(rule.getClass(), ContainsHttpWildcard.class);

		rule = parser.parse("france2.fr,france3.fr,france4.fr,france5.fr,franceo.fr,francetv.fr,francetvinfo.fr,la1ere.fr,pluzz.fr,~sport.francetv.fr##div[id^=\"eShowPub\"");
		Assert.assertEquals(rule.getClass(), EmptyRule.class);

		rule = parser.parse("/cci-ads-");
		Assert.assertEquals(rule.getClass(), Contains.class);

		rule = parser.parse("@@||196.30.218.174/admentor/sirius_sdo_top.htm$subdocument,domain=sharedata.co.za");
		Assert.assertEquals(rule.getClass(), EmptyRule.class);

		rule = parser.parse("192.168.*/images/adv_");
		Assert.assertEquals(rule.getClass(), Contains.class);
	}

}
