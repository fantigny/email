package net.anfoya.mail.browser.javafx.threadlist;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ThreadToolBar extends ToolBar {

	private final Button replyButton;
	private final Button replyAllButton;
	private final Button forwardButton;
	private final Button flagButton;
	private final Button archiveButton;
	private final Button trashButton;

	public ThreadToolBar() {
		replyButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("reply.png"))));
		replyButton.setTooltip(new Tooltip("reply"));

		replyAllButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("replyall.png"))));
		replyAllButton.setTooltip(new Tooltip("reply all"));

		forwardButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("forward.png"))));
		forwardButton.setTooltip(new Tooltip("forward"));

		flagButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("flag.png"))));
		flagButton.setTooltip(new Tooltip("toggle flag"));

		archiveButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("archive.png"))));
		archiveButton.setTooltip(new Tooltip("archive"));

		trashButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("trash.png"))));
		trashButton.setTooltip(new Tooltip("trash"));

		final HBox grow = new HBox();
		HBox.setHgrow(grow, Priority.ALWAYS);

		getChildren().addAll(
				archiveButton, flagButton, trashButton
				, grow
				, replyButton, replyAllButton, forwardButton
				);
	}

	public void setOnReply(final EventHandler<ActionEvent> handler) {
		replyButton.setOnAction(handler);
	}

	public void setOnReplyAll(final EventHandler<ActionEvent> handler) {
		replyAllButton.setOnAction(handler);
	}

	public void setOnForward(final EventHandler<ActionEvent> handler) {
		forwardButton.setOnAction(handler);
	}

	public void setOnToggleFlag(final EventHandler<ActionEvent> handler) {
		flagButton.setOnAction(handler);
	}

	public void setOnArchive(final EventHandler<ActionEvent> handler) {
		archiveButton.setOnAction(handler);
	}

	public void setOnTrash(final EventHandler<ActionEvent> handler) {
		trashButton.setOnAction(handler);
	}
}
