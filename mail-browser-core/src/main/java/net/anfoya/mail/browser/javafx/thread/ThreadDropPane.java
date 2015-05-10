package net.anfoya.mail.browser.javafx.thread;

import static net.anfoya.mail.browser.javafx.threadlist.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import javax.mail.internet.MimeMessage;

import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadDropPane<H extends SimpleThread, M extends SimpleMessage> extends GridPane {
	public static final DataFormat MESSAGE_DATA_FORMAT = new DataFormat(MimeMessage.class.getCanonicalName());
	private final MailService<? extends SimpleSection, ? extends SimpleTag, H, M> mailService;
	private final HBox newThreadBox;
	private final HBox addThreadBox;

	public ThreadDropPane(final MailService<? extends SimpleSection, ? extends SimpleTag, H, M> tagService) {
		this.mailService = tagService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(60);

		addThreadBox = new HBox(0, new Label("add to thread"));
		addThreadBox.setAlignment(Pos.CENTER);
		addThreadBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		addThreadBox.prefWidthProperty().bind(widthProperty());
		addThreadBox.setPrefHeight(50);
		addThreadBox.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				addThreadBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: red");
				event.consume();
			}
		});
		addThreadBox.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				addThreadBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
				event.consume();
			}
		});
		addThreadBox.setOnDragOver(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		addThreadBox.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				addThreads(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		newThreadBox = new HBox(0, new Label("create new thread"));
		newThreadBox.setAlignment(Pos.CENTER);
		newThreadBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		newThreadBox.prefWidthProperty().bind(widthProperty());
		newThreadBox.setPrefHeight(50);
		newThreadBox.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(MESSAGE_DATA_FORMAT)) {
				newThreadBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: red");
				event.consume();
			}
		});
		newThreadBox.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(MESSAGE_DATA_FORMAT)) {
				newThreadBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
				event.consume();
			}
		});
		newThreadBox.setOnDragOver(event -> {
			if (event.getDragboard().hasContent(MESSAGE_DATA_FORMAT)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		newThreadBox.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(MESSAGE_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final M message = (M) event.getDragboard().getContent(MESSAGE_DATA_FORMAT);
				newThread(message);
				event.setDropCompleted(true);
				event.consume();
			}
		});
	}

	private void newThread(final M message) {
		// TODO Auto-generated method stub

	}


	private void addThreads(final Set<H> threads) {
		// TODO Auto-generated method stub

	}

	public void init(final Dragboard db) {
		if (getChildren().contains(newThreadBox)) {
			getChildren().remove(newThreadBox);
		}
		if (getChildren().contains(addThreadBox)) {
			getChildren().remove(addThreadBox);
		}

		if (db.hasContent(MESSAGE_DATA_FORMAT)) {
			addRow(0, newThreadBox);
		}
		if (db.hasContent(DND_THREADS_DATA_FORMAT)) {
			addRow(0, addThreadBox);
		}
	}
}
