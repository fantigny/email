package net.anfoya.movie.search;

import net.anfoya.movie.connector.AllocineConnector;
import net.anfoya.movie.connector.MovieConnector;
import net.anfoya.movie.connector.SimpleMovieConnector;

public class Config {
	public MovieConnector[] getMovieConnectors() {
		return new MovieConnector[] {
				new SimpleMovieConnector(
						"DVD Release"
						, "http://www.dvdrip-fr.com/Site/dernieres_releases.php?type=letter&letter=all"
						, "http://www.dvdrip-fr.com/Site/recherche.php?recherche=%s")
				, new SimpleMovieConnector(
						"eMule Island"
						, "http://www.emule-island.ru/"
						, "http://www.emule-island.ru/recherche.php?categorie=99&find=%s&rechercher=Rechercher&fastr_type=all")
				, new AllocineConnector()
//				, new RottenTomatoesConnector()
//				, new ImDbConnector()
				, new SimpleMovieConnector(
						"YouTube"
						, "https://www.youtube.com"
						, "https://www.youtube.com/results?search_query=official+trailer+%s")
				, new SimpleMovieConnector(
						"C Pas Bien"
						, "https://www.cpasbien.pw"
						, "https://www.cpasbien.pw/recherche/%s.html")
				, new SimpleMovieConnector(
						"Pirate Bay"
						, "https://pirateproxy.sx"
						, "https://pirateproxy.sx/search.php?q=%s")
		};
	}
}
