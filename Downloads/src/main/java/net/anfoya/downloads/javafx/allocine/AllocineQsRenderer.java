package net.anfoya.downloads.javafx.allocine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class AllocineQsRenderer extends GridPane {
	public AllocineQsRenderer(final AllocineQsResult movie) {
	    setHgap(10);
	    setVgap(5);
	    setPadding(new Insets(5));

	    // thumbnail
	    final ImageView imageView = new ImageView(movie.getThumbnailImage());
	    imageView.setPreserveRatio(true);
	    imageView.setFitHeight(100);
	    add(imageView, 0, 0, 1, 3);

	    // title
		final Label title = new Label(movie.getTitle());
		title.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
		title.setAlignment(Pos.CENTER_LEFT);
    	add(title, 1, 0, 1, 1);

		final Label subtitle = new Label();
		subtitle.setFont(Font.font("Verdana", 12));
		subtitle.setAlignment(Pos.CENTER_LEFT);
    	add(subtitle, 1, 1, 1, 1);
    	if (movie.isPerson()) {
	    	// activities
			subtitle.setText(movie.getActivity());
    	} else if (movie.isSerie()) {
	    	// creators
			subtitle.setText(movie.getCreator());
		    if (!movie.getYear().isEmpty()) {
				subtitle.setText(subtitle.getText() + " (" + movie.getYear() + ")");
		    }
    	} else {
	    	// directors (year)
			subtitle.setText(movie.getDirector());
		    if (!movie.getYear().isEmpty()) {
				subtitle.setText(subtitle.getText() + " (" + movie.getYear() + ")");
		    }
    	}

    	// french title if needed
		final Label french = new Label();
		french.setFont(Font.font("Verdana", FontPosture.ITALIC, 14));
		french.setAlignment(Pos.CENTER_LEFT);
	    if (!movie.getTitle().equals(movie.getFrench())) {
			french.setText(movie.getFrench());
	    }
	    add(french, 1, 2, 1, 1);
	}
}
