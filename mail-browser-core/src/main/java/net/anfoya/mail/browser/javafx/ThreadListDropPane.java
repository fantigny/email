package net.anfoya.mail.browser.javafx;

import static net.anfoya.mail.browser.javafx.ThreadListPane.DND_THREADS_DATA_FORMAT;

import java.util.Optional;
import java.util.Set;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadListDropPane<H extends SimpleThread> extends GridPane {
	private final MailService<? extends SimpleSection, ? extends SimpleTag, H> mailService;

	public ThreadListDropPane(final MailService<? extends SimpleSection, ? extends SimpleTag, H> tagService) {
		this.mailService = tagService;

		setPadding(new Insets(5));
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
			archiveBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: red");
			event.consume();
		});
		archiveBox.setOnDragExited(event -> {
			archiveBox.setStyle("-fx-background-color: #DDDDDD; -fx-border-color: black");
			event.consume();
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
		String name = "";
		while(name.isEmpty()) {
			final TextInputDialog inputDialog = new TextInputDialog();
			inputDialog.setTitle("Create new section");
			inputDialog.setHeaderText("");
			inputDialog.setContentText("Section name");
			final Optional<String> response = inputDialog.showAndWait();
			if (!response.isPresent()) {
				return;
			}
			name = response.get();
			if (name.length() < 3) {
				final Alert alertDialog = new Alert(AlertType.ERROR);
				alertDialog.setTitle("Create new section");
				alertDialog.setHeaderText("Section name is too short: " + name);
				alertDialog.setContentText("Section name should be a least 3 letters long.");
				alertDialog.showAndWait();
				name = "";
			}
		}

		try {
			mailService.archive(threads);
		} catch (final MailServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void delete(final Set<H> threads) {
		final Alert alertDialog = new Alert(AlertType.ERROR);
		alertDialog.setTitle("Delete thread");
		alertDialog.setHeaderText("To be implemented");
		alertDialog.setContentText("");
		alertDialog.showAndWait();
		try {
			mailService.delete(threads);
		} catch (final MailServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
