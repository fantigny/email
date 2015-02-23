package net.anfoya.downloads.model;

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
		  			, "/recherche/?q=%s"
		  			, "fichefilm_gen_cfilm=")
			, new Website(
					"Rotten Tomatoes"
					, "www.rottentomatoes.com"
					, ""
					, "/search/?search=%s",".com/m/")
			, new Website(
					"IMDb"
					, "www.imdb.com"
					, ""
					, "/find?ref_=nv_sr_fn&q=%s&s=all"
					, "/title/")
			, new Website(
					"Pirate Bay"
					, "https://pirateproxy.sx"
					, ""
					, "/search.php?q=%s"
					, "")
			, new Website(
					"C Pas Bien"
					, "www.cpasbien.pw"
					, ""
					, "/recherche/%s.html"
					, "")
			, new Website(
					"Google"
					, "www.google.com"
					, ""
					, "/search?q=%s"
					, "")
		};
	}
}
