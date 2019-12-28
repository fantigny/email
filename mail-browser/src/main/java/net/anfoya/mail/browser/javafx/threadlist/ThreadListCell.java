package net.anfoya.mail.browser.javafx.threadlist;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.anfoya.mail.mime.DateHelper;
import net.anfoya.mail.model.Thread;

class ThreadListCell<H extends Thread> extends ListCell<H> {
    private static final Image FLAG = new Image(ThreadListCell.class.getResourceAsStream("/net/anfoya/mail/img/mini_flag.png"));
//    private static final Image UNREAD = new Image(ThreadListCell.class.getResourceAsStream("/net/anfoya/mail/img/mini_unread.png"));

    private static final Color ALMOST_BLACK = Color.web("#444444");

    private final Label sender;
    private final Label date;
    private final Label subject;
    private final Label nbMessages;
    private final VBox iconBox;

	private final HBox senderBox;
	private final HBox subjectBox;
    private final GridPane grid;

	public ThreadListCell() {
		super();
        setPadding(new Insets(0));

		sender = new Label();
		sender.getStyleClass().add("sender");
        sender.setTextFill(ALMOST_BLACK);

		date = new Label();
		date.getStyleClass().add("date");
		date.setMinWidth(Label.USE_PREF_SIZE);

		final HBox empty1 = new HBox();
		empty1.setMinWidth(5);
		HBox.setHgrow(empty1, Priority.ALWAYS);
		senderBox = new HBox(sender, empty1, date);
		senderBox.setPadding(new Insets(0, 0, 2, 0));
		senderBox.setAlignment(Pos.BASELINE_LEFT);

		subject = new Label();
		subject.getStyleClass().add("subject");

        nbMessages = new Label();
        nbMessages.getStyleClass().add("date");

		final HBox empty2 = new HBox();
		empty2.setMinWidth(5);
		HBox.setHgrow(empty2, Priority.ALWAYS);
		subjectBox = new HBox(subject, empty2, nbMessages);
		subjectBox.setPadding(new Insets(0, 0, 2, 0));
		subjectBox.setAlignment(Pos.BASELINE_LEFT);

		iconBox = new VBox();
		iconBox.setSpacing(3);
		iconBox.setPadding(new Insets(3, 2, 0, 2));
		iconBox.setMinWidth(11);
		iconBox.setMaxWidth(11);

		grid = buildGridPane(null);
		grid.prefWidthProperty().bind(widthProperty());
	}

	@Override
    public void updateItem(final H thread, final boolean empty) {
        super.updateItem(thread, empty);
    	setText(null);

    	if (empty) {
            setGraphic(null);
        } else {
        	final int nbMess = thread.getMessageIds().size();

    		sender.setText(thread.getSender());
        	nbMessages.setText(nbMess > 1? "(x" + nbMess + ")": "");
            nbMessages.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        	subject.setText(thread.getSubject());
	        if (thread.getId().equals(Thread.PAGE_TOKEN_ID)) {
	        	date.setText("");
	        } else {
	        	date.setText(new DateHelper(thread.getDate()).format());
	        }

	        sender.setTextFill(thread.isUnread()? Color.FIREBRICK: ALMOST_BLACK);

	        iconBox.getChildren().clear();
//        	if (thread.isUnread()) {
//        		iconBox.getChildren().add(new ImageView(UNREAD));
//        	}
	        if (thread.isFlagged()) {
        		iconBox.getChildren().add(new ImageView(FLAG));
        	}

        	setGraphic(grid);
        }
	}

	public GridPane buildGridPane(H thread) {
		final GridPane grid = new GridPane();
		grid.setHgap(3);
		grid.setPadding(new Insets(3));
		grid.add(iconBox, 0, 0, 1, 2);
		grid.add(senderBox, 1, 0);
		grid.add(subjectBox, 1, 1);

		GridPane.setHgrow(senderBox, Priority.ALWAYS);
		GridPane.setHgrow(subjectBox, Priority.ALWAYS);

		updateItem(thread, thread == null);
		return grid;
	}
}