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

public class ThreadListToolBar extends ToolBar {

	private final Button replyButton;
	private final Button replyAllButton;
	private final Button forwardButton;
	private final Button flagButton;
	private final Button archiveButton;
	private final Button trashButton;
	private final Button spamButton;

	public ThreadListToolBar() {
		setMinHeight(27);
		setMaxHeight(27);

		replyButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/reply.png"))));
		replyButton.setTooltip(new Tooltip("reply"));

		replyAllButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/replyall.png"))));
		replyAllButton.setTooltip(new Tooltip("reply all"));

		forwardButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/forward.png"))));
		forwardButton.setTooltip(new Tooltip("forward"));

		flagButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/flag.png"))));
		flagButton.setTooltip(new Tooltip("toggle flag"));

		archiveButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/archive.png"))));
		archiveButton.setTooltip(new Tooltip("archive"));

		trashButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/trash.png"))));
		trashButton.setTooltip(new Tooltip("trash"));

		spamButton = new Button("", new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/spam.png"))));
		spamButton.setTooltip(new Tooltip("toggle spam"));

		final HBox grow = new HBox();
		HBox.setHgrow(grow, Priority.ALWAYS);

		getItems().addAll(
				archiveButton, flagButton, spamButton, trashButton
				, grow
				, replyButton, replyAllButton, forwardButton
				);

		setFocusTraversable(true);
		focusTraversableProperty().addListener((ov, o, n) -> getItems().forEach(node -> node.setFocusTraversable(n)));
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

	public void setOnSpam(final EventHandler<ActionEvent> handler) {
		spamButton.setOnAction(handler);
	}
}
