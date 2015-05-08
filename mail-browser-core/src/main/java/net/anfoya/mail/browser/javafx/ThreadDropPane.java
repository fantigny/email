package net.anfoya.mail.browser.javafx;

import static net.anfoya.mail.browser.javafx.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import javax.mail.internet.MimeMessage;

import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadDropPane<H extends SimpleThread> extends GridPane {
	private static final DataFormat MESSAGE_DATA_FORMAT = new DataFormat(MimeMessage.class.getCanonicalName());
	private final MailService<? extends SimpleSection, ? extends SimpleTag, H> mailService;

	public ThreadDropPane(final MailService<? extends SimpleSection, ? extends SimpleTag, H> tagService) {
		this.mailService = tagService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(60);

		final HBox archiveBox = new HBox(0, new Label("add to thread"));
		archiveBox.setAlignment(Pos.CENTER);
		archiveBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		archiveBox.prefWidthProperty().bind(widthProperty());
		archiveBox.setPrefHeight(50);
		archiveBox.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				archiveBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: red");
				event.consume();
			}
		});
		archiveBox.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				archiveBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
				event.consume();
			}
		});
		archiveBox.setOnDragOver(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		archiveBox.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				addThreads(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final HBox deleteBox = new HBox(0, new Label("extract to new thread"));
		deleteBox.setAlignment(Pos.CENTER);
		deleteBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		deleteBox.prefWidthProperty().bind(widthProperty());
		deleteBox.setPrefHeight(50);
		deleteBox.setOnDragEntered(event -> {
			deleteBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: red");
			event.consume();
		});
		deleteBox.setOnDragExited(event -> {
			deleteBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
			event.consume();
		});
		deleteBox.setOnDragOver(event -> {
			if (event.getDragboard().hasContent(MESSAGE_DATA_FORMAT)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		deleteBox.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(MESSAGE_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final MimeMessage message = (MimeMessage) event.getDragboard().getContent(MESSAGE_DATA_FORMAT);
				remove(message);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		addRow(0, archiveBox, deleteBox);
	}

	private void remove(final MimeMessage message) {
		// TODO Auto-generated method stub

	}


	private void addThreads(final Set<H> threads) {
		// TODO Auto-generated method stub

	}
}
