package net.anfoya.downloads.javafx.allocine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocineMovieRenderer extends GridPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineMovieRenderer.class);

	public AllocineMovieRenderer(final AllocineMovie movie) {
	    setHgap(10);
	    setVgap(10);
	    setPadding(new Insets(0, 10, 0, 10));

		try {
			final ImageView view = new ImageView(new Image(new BufferedInputStream(new URL(movie.getThumbnail()).openStream())));
		    add(view, 0, 0, 1, 2);
		} catch (final IOException e) {
			LOGGER.warn("loading thunbnail {}", movie.getThumbnail(), e);
		}
		
		Label title = new Label(movie.getOriginalTitle());
		title.setStyle("-fx-font: 16 arial; -fx-base: #b6e7c9;");
		title.setAlignment(Pos.CENTER_LEFT);
    	add(title, 1, 0, 1, 1);
	    if (!movie.getOriginalTitle().equals(movie.getFrenchTitle())) {
			Label french = new Label(movie.getFrenchTitle());
			french.setAlignment(Pos.CENTER_LEFT);
		    add(french, 1, 1, 1, 1);
	    }
	}
}
