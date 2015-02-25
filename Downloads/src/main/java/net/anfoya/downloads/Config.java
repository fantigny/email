package net.anfoya.downloads;

import net.anfoya.tools.model.Website;

public class Config {
	public Website[] getWebsites() {
		return new Website[] {
		  	new Website(
		  			"DVD Release"
		  			, "www.dvdrip-fr.com"
		  			, "/Site/dernieres_releases.php?type=letter"
		  			, "/Site/recherche.php?recherche=%s"
		  			, "")
		  	, new Website(
		  			"AlloCine"
		  			, "www.allocine.fr"
		  			, "/film/aucinema/"
		  			, "/recherche/?q=%s"
		  			, "fichefilm_gen_cfilm=")
		};
	}
}
