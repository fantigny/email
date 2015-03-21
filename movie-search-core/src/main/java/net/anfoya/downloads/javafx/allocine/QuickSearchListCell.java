package net.anfoya.downloads.javafx.allocine;

import net.anfoya.movie.connector.QuickSearchVo;
import javafx.scene.control.ListCell;

public class QuickSearchListCell extends ListCell<QuickSearchVo> {
    @Override
    protected void updateItem(final QuickSearchVo qsResult, final boolean empty) {
    	super.updateItem(qsResult, empty);

    	if (qsResult == null || empty) {
            setGraphic(null);
        } else {
        	setGraphic(new QuickSearchRenderer(qsResult));
        }
   }
}
