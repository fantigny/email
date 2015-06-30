package net.anfoya.mail.browser.javafx.threadlist;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.effect.Light.Distant;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.anfoya.mail.browser.mime.DateHelper;
import net.anfoya.mail.service.Thread;

class ThreadListCell<H extends Thread> extends ListCell<H> {
    private static final Image EMPTY = new Image(ThreadListCell.class.getResourceAsStream("mini_empty.png"));
    private static final Image STAR = new Image(ThreadListCell.class.getResourceAsStream("mini_star.png"));
    private static final Image UNREAD = new Image(ThreadListCell.class.getResourceAsStream("mini_unread.png"));

    private static final Color ALMOST_BLACK = Color.web("#444444");

    private final Label sender;
    private final Label subject;
    private final Label date;
    private final GridPane grid;
    private final VBox iconBox;

	public ThreadListCell() {
		super();
        setPadding(new Insets(0));
        getStyleClass().add("thread-list-cell");

		sender = new Label();
		sender.getStyleClass().add("sender");
        sender.setTextFill(ALMOST_BLACK);

		date = new Label();
		date.getStyleClass().add("date");
		date.setMinWidth(Label.USE_PREF_SIZE);

		final HBox empty = new HBox();
		empty.setMinWidth(5);
		HBox.setHgrow(empty, Priority.ALWAYS);
		final HBox senderBox = new HBox(sender, empty, date);
		senderBox.setPadding(new Insets(0, 0, 2, 0));
		senderBox.prefWidthProperty().bind(this.widthProperty().add(-20));

		subject = new Label();
		subject.getStyleClass().add("subject");

		iconBox = new VBox();
		iconBox.setSpacing(3);
		iconBox.setPadding(new Insets(3, 2, 0, 2));

		grid = new GridPane();
		grid.setHgap(3);
		grid.setPadding(new Insets(3));
		grid.prefWidthProperty().bind(this.widthProperty().add(-20));
		grid.add(iconBox, 0, 0, 1, 2);
		grid.add(senderBox, 1, 0);
		grid.add(subject, 1, 1);

        final Distant light = new Distant();
        light.setAzimuth(-135.0f);
	}

	@Override
    public void updateItem(final H thread, final boolean empty) {
        super.updateItem(thread, empty);
    	setText(null);

    	if (empty) {
            setGraphic(null);
        } else {
        	sender.setText(thread.getSender());
        	date.setText(new DateHelper(thread.getDate()).format());
        	subject.setText(thread.getSubject());

	        sender.setTextFill(thread.isUnread()? Color.FIREBRICK: ALMOST_BLACK);

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

        	setGraphic(grid);
        }
	}
}