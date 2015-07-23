package net.anfoya.mail.browser.javafx.message;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.web.WebViewFitContent;
import net.anfoya.mail.browser.javafx.thread.ThreadDropPane;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.mime.DateHelper;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import netscape.javascript.JSObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagePane<M extends Message, C extends Contact> extends VBox {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessagePane.class);

	private final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService;
	private final BooleanProperty expanded;
	private final MessageHelper helper;

	private boolean collapsible;

	private final Text titleText;
	private final Text dateText;
	private final WebViewFitContent bodyView;

	private final String messageId;
	private M message;
	private Task<String> loadTask;

	private EventHandler<ActionEvent> updateHandler;

	public MessagePane(final String messageId, final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService) {
		this.mailService = mailService;
		this.messageId = messageId;

		collapsible = true;
		helper = new MessageHelper();

		bodyView = new WebViewFitContent();
		bodyView.getEngine().setCreatePopupHandler(handler -> bodyView.getEngine());
		bodyView.getEngine().locationProperty().addListener((ov, o, n) -> {
			if (o != null) {
				Platform.runLater(() -> bodyView.getEngine().getLoadWorker().cancel());
				handleHyperlink(n);
			}
		});

		expanded = new SimpleBooleanProperty(true);
		expanded.addListener((ov, oldVal, newVal) -> {
			if (collapsible) {
				final double height = newVal? bodyView.getPrefHeight(): 0;
				bodyView.setMinHeight(height);
				bodyView.setMaxHeight(height);
				autosize();
			}
		});

		titleText = new Text("loading...");
		dateText = new Text();

		final HBox empty = new HBox();
		HBox.setHgrow(empty, Priority.ALWAYS);

		final HBox titlePane = new HBox(titleText, empty, dateText);
		titlePane.getStyleClass().add("message-title-pane");
		titlePane.setPadding(new Insets(5));
		titlePane.setAlignment(Pos.CENTER_LEFT);
		titlePane.setMinHeight(30);
		titlePane.setOnMouseClicked(event -> expanded.set(!expanded.get()));

		getChildren().addAll(titlePane, bodyView);
		setOnDragDetected(event -> {
	        final ClipboardContent content = new ClipboardContent();
	        content.put(ThreadDropPane.MESSAGE_DATA_FORMAT, message);
	        final Dragboard db = startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
	}

	private void handleHyperlink(final String location) {
		try {
			final URI uri = new URI(location);
			final String scheme = uri.getScheme();
			if (scheme.equals("mailto")) {
				new MailComposer<M, C>(mailService, updateHandler).newMessage(uri.getSchemeSpecificPart());
			} else {
				Desktop.getDesktop().browse(uri);
			}
		} catch (final Exception e) {
			LOGGER.error("handling link \"{}\"", location, e);
		}
	}

	public synchronized void load() {
		if (loadTask != null) {
			//already loading;
			return;
		}
		loadTask = new Task<String>() {
			@Override
			protected String call() throws MailException, MessagingException, IOException, URISyntaxException {
				message = mailService.getMessage(messageId);
			    return helper.toHtml(message.getMimeMessage());
			}
		};
		loadTask.setOnFailed(event -> {
			LOGGER.error("loading message id {}", messageId, event.getSource().getException());
		});
		loadTask.setOnSucceeded(event -> {
			refreshTitle();
			bodyView.getEngine().loadContent(loadTask.getValue());
			((JSObject) bodyView.getEngine().executeScript("window")).setMember("attLoader", new AttachmentLoader<M>(mailService, messageId));
		});
		ThreadPool.getInstance().submitHigh(loadTask);
	}

	private void refreshTitle() {
		try {
			final MimeMessage mimeMessage = message.getMimeMessage();

			final StringBuilder title = new StringBuilder();
			title.append(String.join(", ", helper.getMailAddresses(mimeMessage.getFrom())));
			title.append(" to ").append(String.join(", ", helper.getMailAddresses(mimeMessage.getRecipients(MimeMessage.RecipientType.TO))));
			titleText.setText(title.toString());
			dateText.setText(new DateHelper(mimeMessage.getSentDate()).format());
		} catch (final MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setScrollHandler(final EventHandler<ScrollEvent> handler) {
		bodyView.setScrollHandler(handler);
	}

	public boolean isExpanded() {
		return expanded.get();
	}

	public void setExpanded(final boolean expanded) {
		this.expanded.set(expanded);
	}

	public BooleanProperty expandedProperty() {
		return expanded;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setCollapsible(final boolean collapsible) {
		this.collapsible = collapsible;
	}

	public boolean isCollapsble() {
		return collapsible;
	}

	public void setUpdateHandler(final EventHandler<ActionEvent> handler) {
		this.updateHandler = handler;
	}
}
