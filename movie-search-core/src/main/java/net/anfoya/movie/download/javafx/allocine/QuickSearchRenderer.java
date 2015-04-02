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
	public QuickSearchRenderer(final MovieVo qsVo) {
	    setHgap(10);
	    setVgap(5);
	    setPadding(new Insets(5));

	    // thumbnail
	    final ImageView imageView = new ImageView(qsVo.getThumbnail());
	    imageView.setPreserveRatio(true);
	    imageView.setFitHeight(100);
	    add(imageView, 0, 0, 1, 3);

	    // title
		final Label title = new Label(qsVo.getName());
		title.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
		title.setAlignment(Pos.CENTER_LEFT);
    	add(title, 1, 0, 1, 1);

		final Label subtitle = new Label();
		subtitle.setFont(Font.font("Verdana", 12));
		subtitle.setAlignment(Pos.CENTER_LEFT);
    	add(subtitle, 1, 1, 1, 1);
    	switch (qsVo.getType()) {
		case PERSON:
	    	// activities (nationality)
			subtitle.setText(qsVo.getActivity());
			if (!qsVo.getCountry().isEmpty()) {
				subtitle.setText(subtitle.getText() + " (" + qsVo.getCountry() + ")");
		    }
			break;
		case SERIE:
	    	// creators (start year)
			subtitle.setText(qsVo.getCreator());
		    if (!qsVo.getYear().isEmpty()) {
				subtitle.setText(subtitle.getText() + " (" + qsVo.getYear() + ")");
		    }
		    break;
		default:
	    	// directors (production year)
			subtitle.setText(qsVo.getDirector());
		    if (!qsVo.getYear().isEmpty()) {
				subtitle.setText(subtitle.getText() + " (" + qsVo.getYear() + ")");
		    }
			break;
    	}

    	// french title if needed
		final Label french = new Label();
		french.setFont(Font.font("Verdana", FontPosture.ITALIC, 14));
		french.setAlignment(Pos.CENTER_LEFT);
	    if (!qsVo.getName().equals(qsVo.getFrench())) {
			french.setText(qsVo.getFrench());
	    }
	    add(french, 1, 2, 1, 1);
	}
}
