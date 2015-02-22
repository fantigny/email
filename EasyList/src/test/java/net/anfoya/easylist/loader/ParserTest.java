package net.anfoya.easylist.loader;

import net.anfoya.easylist.model.rules.Contains;
import net.anfoya.easylist.model.rules.ContainsHttpWildcard;
import net.anfoya.easylist.model.rules.EmptyRule;
import net.anfoya.easylist.model.rules.Rule;

import org.junit.Assert;
import org.junit.Test;

public class ParserTest {

	@Test
	public void pattern() {
		Rule rule;
		Parser parser = new Parser();
		
		rule = parser.parse("imdb.com###top_rhs_1_wrapper");
		Assert.assertTrue(rule instanceof EmptyRule);

		rule = parser.parse("||cacheserve.*/promodisplay/");
		Assert.assertTrue(rule instanceof ContainsHttpWildcard);

		rule = parser.parse("france2.fr,france3.fr,france4.fr,france5.fr,franceo.fr,francetv.fr,francetvinfo.fr,la1ere.fr,pluzz.fr,~sport.francetv.fr##div[id^=\"eShowPub\"");
		Assert.assertTrue(rule instanceof EmptyRule);

		rule = new Contains("/cci-ads-");
		Assert.assertTrue(rule instanceof Contains);
	}

}
