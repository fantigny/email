package net.anfoya.downloads.javafx.allocine;

import javafx.scene.control.ListCell;

public class AllocineListCell extends ListCell<AllocineMovie> {
    @Override
    protected void updateItem(final AllocineMovie movie, final boolean empty) {
    	super.updateItem(movie, empty);

    	if (movie == null || empty) {
            setGraphic(null);
        } else {
        	setGraphic(new AllocineMovieRenderer(movie));
        }
   }
}
