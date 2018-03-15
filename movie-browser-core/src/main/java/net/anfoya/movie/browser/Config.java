package net.anfoya.movie.browser;

import net.anfoya.movie.browser.model.Website;

public class Config {
	public Website[] getWebsites() {
		return new Website[] {
			  new Website(
					  "AlloCine"
					  , "www.allocine.fr"
					  , "/recherche/?q=%s"
					  , "fichefilm_gen_cfilm=")
			, new Website(
					"Rotten Tomatoes"
					, "www.rottentomatoes.com"
					, "/search/?search=%s"
					,".com/m/")
			, new Website(
					"IMDb"
					, "www.imdb.com"
					, "/find?ref_=nv_sr_fn&q=%s&s=all"
					,"/title/")
			, new Website("YouTube"
					, "http://www.youtube.com"
					, "/results?search_query=official+trailer+%s"
					, "")
			, new Website(
					"Google"
					, "www.google.com"
					, "/search?q=%s"
					, ""
					, true)
		};
	}
}
