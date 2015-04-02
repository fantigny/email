package net.anfoya.movie.download.javafx.allocine;

import net.anfoya.movie.connector.MovieVo;
import javafx.scene.control.ListCell;

public class QuickSearchListCell extends ListCell<MovieVo> {
    @Override
    protected void updateItem(final MovieVo qsResult, final boolean empty) {
    	super.updateItem(qsResult, empty);

    	if (qsResult == null || empty) {
            setGraphic(null);
        } else {
        	setGraphic(new QuickSearchRenderer(qsResult));
        }
   }
}
