package net.anfoya.mail.browser.javafx.threadlist;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import net.anfoya.mail.service.Thread;

class ThreadListCell<H extends Thread> extends ListCell<H> {
    private static final Image EMPTY = new Image(ThreadListCell.class.getResourceAsStream("mini_empty.png"));
    private static final Image STAR = new Image(ThreadListCell.class.getResourceAsStream("mini_star.png"));
    private static final Image UNREAD = new Image(ThreadListCell.class.getResourceAsStream("mini_unread.png"));

    private final Label sender;
    private final Label subject;
    private final GridPane grid;
    private final VBox iconBox;

	public ThreadListCell() {
		super();
        setPadding(new Insets(0));

		sender = new Label();
		subject = new Label();
		subject.setStyle("-fx-font-size: 12px");

		iconBox = new VBox();
		iconBox.setSpacing(3);
		iconBox.setPadding(new Insets(3, 2, 0, 2));

		grid = new GridPane();
		grid.setPadding(new Insets(3));
		grid.setHgap(3);
		grid.add(iconBox, 0, 0, 1, 2);
		grid.add(sender, 1, 0);
		grid.add(subject, 1, 1);
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
        		sender.setStyle("-fx-font-size: 13px; -fx-font-weight: bold");
        	} else {
        		sender.setStyle("-fx-font-size: 13px");
        	}

    		iconBox.getChildren().clear();
        	if (thread.isUnread()) {
        		iconBox.getChildren().add(new ImageView(UNREAD));
        	}
        	if (thread.isStarred()) {
        		iconBox.getChildren().add(new ImageView(STAR));
        	}
        	if (iconBox.getChildren().isEmpty()) {
        		iconBox.getChildren().add(new ImageView(EMPTY));
        	}

        	grid.setPrefWidth(getListView().getWidth() - getListView().getInsets().getLeft() - getListView().getInsets().getRight());
        	setGraphic(grid);
        }
	}
}