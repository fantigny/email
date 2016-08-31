package net.anfoya.movie.search;

import net.anfoya.movie.connector.AllocineConnector;
import net.anfoya.movie.connector.MovieConnector;
import net.anfoya.movie.connector.SimpleMovieConnector;

public class Config {
	public MovieConnector[] getMovieConnectors() {
		return new MovieConnector[] {
				new SimpleMovieConnector(
						"eMule Island"
						, "http://www.emule-island.ru/"
						, "http://www.emule-island.ru/recherche.php?categorie=99&find=%s&rechercher=Rechercher&fastr_type=all")
				, new SimpleMovieConnector(
						"DVD Release"
						, "http://www.dvdrip-fr.com/"
						, "http://www.dvdrip-fr.com/Site/recherche.php?recherche=%s")
				, new AllocineConnector()
//				, new RottenTomatoesConnector()
//				, new ImDbConnector()
				, new SimpleMovieConnector(
						"YouTube"
						, "https://www.youtube.com"
						, "https://www.youtube.com/results?search_query=%s+official+trailer")
				, new SimpleMovieConnector(
						"C Pas Bien"
						, "http://www.cpasbien.io"
						, "http://www.cpasbien.io/recherche/%s.html")
				, new SimpleMovieConnector(
						"Pirate Bay"
						, "https://pirateproxy.sx"
						, "https://pirateproxy.sx/search/%s/0/99/0")
		};
	}
}
