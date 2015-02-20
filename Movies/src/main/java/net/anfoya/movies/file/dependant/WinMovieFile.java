package net.anfoya.movies.file.dependant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import net.anfoya.movies.file.AbstractMovieFile;
import net.anfoya.movies.file.MovieFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WinMovieFile extends AbstractMovieFile implements MovieFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(WinMovieFile.class);

	public WinMovieFile(URL url) {
		super(url);
	}

	public WinMovieFile(String url) throws MalformedURLException {
		super(url);
	}

	@Override
	public void showInFileExplorer() throws IOException {
		String localPath = getUncPath();
		LOGGER.info("opening file manager for: {}", localPath);
		Process process = Runtime.getRuntime().exec(new String[] { "explorer", "/select,", localPath });
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line; while((line = br.readLine()) != null) LOGGER.debug(line);
	}

	@Override
	public void open() throws IOException {
		String localPath = getUncPath();
		LOGGER.info("starting: {}", localPath);
        Process process = Runtime.getRuntime().exec(new String[] { "explorer", localPath } );
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line; while((line = br.readLine()) != null) LOGGER.debug(line);
	}
}
