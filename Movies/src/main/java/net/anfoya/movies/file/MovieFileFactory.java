package net.anfoya.movies.file;

import java.net.MalformedURLException;
import java.net.URL;

import net.anfoya.movies.file.dependant.MacMovieFile;
import net.anfoya.movies.file.dependant.UnxMovieFile;
import net.anfoya.movies.file.dependant.WinMovieFile;
import net.anfoya.tools.OperatingSystem;

public class MovieFileFactory {
	private static final OperatingSystem.Family OS_FAMILY = OperatingSystem.getInstance().getFamily();

	private MovieFileFactory() {}

	public static MovieFile getFile(final URL url) {
		switch(OS_FAMILY) {
		case MAC:
			return new MacMovieFile(url);
		case WIN:
			return new WinMovieFile(url);
		case UNX:
			return new UnxMovieFile(url);
		case UKN:
			return new WinMovieFile(url);
		}
		return null;
	}

	public static MovieFile getFile(final String url) throws MalformedURLException {
		switch(OS_FAMILY) {
		case MAC:
			return new MacMovieFile(url);
		case WIN:
			return new WinMovieFile(url);
		case UNX:
			return new UnxMovieFile(url);
		case UKN:
			return new WinMovieFile(url);
		}
		return null;
	}
}
