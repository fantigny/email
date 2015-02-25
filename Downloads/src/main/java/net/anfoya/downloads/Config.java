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
/*			, new Website(
					"Rotten Tomatoes"
					, "www.rottentomatoes.com"
					, "/browse/in-theaters/"
					, "/search/?search=%s"
					,".com/m/")
			, new Website(
					"IMDb"
					, "www.imdb.com"
					, "/movies-in-theaters/?ref_=nv_mv_inth_1"
					, "/find?ref_=nv_sr_fn&q=%s&s=all"
					, "/title/")
			, new Website(
					"YouTube"
					, "www.youtube.com"
					, "/results?search_query=trailer+%s"
					, "/watch?v=")
			, new Website(
					"Pirate Bay"
					, "https://pirateproxy.sx"
					, "/search/"
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
*/
		};
	}
}
