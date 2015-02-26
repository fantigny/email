package net.anfoya.downloads.javafx.allocine;

import javafx.scene.control.ListCell;

public class AllocineQsListCell extends ListCell<AllocineQsResult> {
    @Override
    protected void updateItem(final AllocineQsResult qsResult, final boolean empty) {
    	super.updateItem(qsResult, empty);

    	if (qsResult == null || empty) {
            setGraphic(null);
        } else {
        	setGraphic(new AllocineQsRenderer(qsResult));
        }
   }
}
