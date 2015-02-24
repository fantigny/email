package net.anfoya.downloads.javafx.allocine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class AllocineMovieRenderer extends GridPane {

	public AllocineMovieRenderer(final AllocineMovie movie) {

		if (!movie.getThumbnail().isEmpty()) {
			try {
				final ImageView view = new ImageView(new Image(new BufferedInputStream(new URL(movie.getThumbnail()).openStream())));
			    add(view, 0, 0, 1, 2);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	    add(new Label(movie.getOriginalTitle()), 1, 1, 1, 1);
	    add(new Label(movie.getFrenchTitle()), 1, 0, 1, 1);
	}
}
