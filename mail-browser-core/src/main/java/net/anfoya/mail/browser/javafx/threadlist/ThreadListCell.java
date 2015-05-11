package net.anfoya.mail.browser.javafx.threadlist;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import net.anfoya.mail.service.Thread;

class ThreadListCell<H extends Thread> extends ListCell<H> {
    private final Label sender;
    private final Label subject;
    private final GridPane grid;

	public ThreadListCell() {
		super();

		sender = new Label();
		sender.setFont(new Font(14));
		subject = new Label();
		subject.setFont(new Font(11));

		grid = new GridPane();
		grid.addRow(0, sender);
		grid.addRow(1, subject);
	}

	@Override
    public void updateItem(final H thread, final boolean empty) {
        super.updateItem(thread, empty);
    	setText(null);

    	if (empty) {
            setGraphic(null);
        } else {
        	sender.setText(thread.getSender());
        	subject.setText(thread.getSubject());

        	if (thread.isUnread()) {
        		sender.setStyle("-fx-font-weight: bold");
        	} else {
        		sender.setStyle("-fx-font-weight: normal");
        	}

        	setGraphic(grid);
        }
	}
}