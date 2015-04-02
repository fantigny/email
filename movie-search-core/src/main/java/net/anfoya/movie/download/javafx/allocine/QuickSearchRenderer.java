package net.anfoya.movie.download.javafx.allocine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import net.anfoya.movie.connector.MovieVo;

public class QuickSearchRenderer extends GridPane {

	public QuickSearchRenderer(final MovieVo movieVo) {
	    setHgap(10);
	    setVgap(5);
	    setPadding(new Insets(5));

	    // thumbnail
	    final ImageView imageView = new ImageView(movieVo.getThumbnail());
	    imageView.setPreserveRatio(true);
	    imageView.setFitHeight(100);
	    add(imageView, 0, 0, 1, 3);

	    // title
		final Label title = new Label(movieVo.getName());
		title.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
		title.setAlignment(Pos.CENTER_LEFT);
    	add(title, 1, 0, 1, 1);

		final Label subtitle = new Label();
		subtitle.setFont(Font.font("Verdana", 12));
		subtitle.setAlignment(Pos.CENTER_LEFT);
    	add(subtitle, 1, 1, 1, 1);
    	switch (movieVo.getType()) {
		case PERSON:
	    	// activities (nationality)
			subtitle.setText(movieVo.getActivity());
			if (!movieVo.getCountry().isEmpty()) {
				subtitle.setText(subtitle.getText() + " (" + movieVo.getCountry() + ")");
		    }
			break;
		case SERIE:
	    	// creators (start year)
			subtitle.setText(movieVo.getCreator());
		    if (!movieVo.getYear().isEmpty()) {
				subtitle.setText(subtitle.getText() + " (" + movieVo.getYear() + ")");
		    }
		    break;
		default:
	    	// directors (production year)
			subtitle.setText(movieVo.getDirector());
		    if (!movieVo.getYear().isEmpty()) {
				subtitle.setText(subtitle.getText() + " (" + movieVo.getYear() + ")");
		    }
			break;
    	}

    	// french title if needed
		final Label french = new Label();
		french.setFont(Font.font("Verdana", FontPosture.ITALIC, 14));
		french.setAlignment(Pos.CENTER_LEFT);
	    if (!movieVo.getName().equals(movieVo.getFrench())) {
			french.setText(movieVo.getFrench());
	    }
	    add(french, 1, 2, 1, 1);
	}
}
