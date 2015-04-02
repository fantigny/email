package net.anfoya.movie.search;

import net.anfoya.movie.connector.AllocineConnector;
import net.anfoya.movie.connector.ImDbConnector;
import net.anfoya.movie.connector.MovieConnector;
import net.anfoya.movie.connector.RottenTomatoesConnector;
import net.anfoya.movie.connector.SimpleMovieConnector;

public class Config {
	public MovieConnector[] getMovieConnectors() {
		return new MovieConnector[] {
				new SimpleMovieConnector(
						"DVD Release"
						, "http://www.dvdrip-fr.com/Site/dernieres_releases.php?type=letter&letter=all"
						, "http://www.dvdrip-fr.com/Site/recherche.php?recherche=%s")
				, new AllocineConnector()
				, new RottenTomatoesConnector()
				, new ImDbConnector()
				, new SimpleMovieConnector(
						"YouTube"
						, "https://www.youtube.com"
						, "https://www.youtube.com/results?search_query=official+trailer+%s")
				, new SimpleMovieConnector(
						"Pirate Bay"
						, "https://pirateproxy.sx"
						, "https://pirateproxy.sx/search.php?q=%s")
				, new SimpleMovieConnector(
						"C Pas Bien"
						, "https://www.cpasbien.pw"
						, "https://www.cpasbien.pw/recherche/%s.html")
				, new SimpleMovieConnector(
						"Google"
						, "https://www.google.com"
						, "https://www.google.com/search?q=%s")
		};
	}
}
