package net.anfoya.mail.browser.javafx.dnd;

import static net.anfoya.mail.browser.javafx.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Set;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadListDropPane<H extends SimpleThread> extends GridPane {
	private final MailService<? extends SimpleSection, ? extends SimpleTag, H, ? extends SimpleMessage> mailService;

	public ThreadListDropPane(final MailService<? extends SimpleSection, ? extends SimpleTag, H, ? extends SimpleMessage> tagService) {
		this.mailService = tagService;

		setVgap(2);
		setHgap(2);
		setAlignment(Pos.BOTTOM_CENTER);
		setOpacity(0.9);

		setMaxHeight(60);

		final HBox archiveBox = new HBox(0, new Label("archive"));
		archiveBox.setAlignment(Pos.CENTER);
		archiveBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		archiveBox.setPrefWidth(200);
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
				archive(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		final HBox deleteBox = new HBox(0, new Label("delete"));
		deleteBox.setAlignment(Pos.CENTER);
		deleteBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
		deleteBox.setPrefWidth(200);
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
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				event.acceptTransferModes(TransferMode.ANY);
				event.consume();
			}
		});
		deleteBox.setOnDragDropped(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) event.getDragboard().getContent(DND_THREADS_DATA_FORMAT);
				delete(threads);
				event.setDropCompleted(true);
				event.consume();
			}
		});

		addRow(0, archiveBox, deleteBox);
	}

	private void archive(final Set<H> threads) {
		try {
			mailService.archive(threads);
		} catch (final MailServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void delete(final Set<H> threads) {
		try {
			mailService.delete(threads);
		} catch (final MailServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
