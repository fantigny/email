package net.anfoya.mail.browser.javafx.message;

import java.io.IOException;
import java.net.URISyntaxException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.web.WebViewFitContent;
import net.anfoya.mail.browser.javafx.thread.ThreadDropPane;
import net.anfoya.mail.browser.mime.MimeMessageHelper;
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.mail.model.SimpleTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagePane<M extends SimpleMessage> extends VBox {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessagePane.class);

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M, ? extends SimpleContact> mailService;
	private final BooleanProperty expanded;
	private final MimeMessageHelper helper;

	private boolean collapsible;

	private final Text titleText;
	private final WebViewFitContent bodyView;

	private final String messageId;
	private M message;
	private Task<String> loadTask;

	public MessagePane(final String messageId, final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M, ? extends SimpleContact> mailService) {
		this.mailService = mailService;
		this.messageId = messageId;

		helper = new MimeMessageHelper();
		collapsible = true;

		bodyView = new WebViewFitContent();

		expanded = new SimpleBooleanProperty(true);
		expanded.addListener((ov, oldVal, newVal) -> {
			if (collapsible) {
				final double height = newVal? bodyView.getPrefHeight(): 0;
				bodyView.setMinHeight(height);
				bodyView.setMaxHeight(height);
				autosize();
			}
		});

		final HBox titlePane = new HBox();
		titlePane.setPadding(new Insets(5));
		titlePane.setAlignment(Pos.CENTER_LEFT);
		titlePane.setMinHeight(30);
		titlePane.setOnMouseClicked(event -> {
			expanded.set(!expanded.get());
		});

		titleText = new Text("loading...");
		titlePane.getChildren().add(titleText);

		getChildren().addAll(titlePane, bodyView);
		setOnDragDetected(event -> {
	        final ClipboardContent content = new ClipboardContent();
	        content.put(ThreadDropPane.MESSAGE_DATA_FORMAT, message);
	        final Dragboard db = startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
	}

	public synchronized void load() {
		if (loadTask != null) {
			//already loaded;
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
			bodyView.loadContent(loadTask.getValue());
		});
		ThreadPool.getInstance().submitHigh(loadTask);
	}

	private void refreshTitle() {
		try {
			final MimeMessage mimeMessage = message.getMimeMessage();

			final StringBuilder title = new StringBuilder();
			title.append(mimeMessage.getSentDate());
			title.append(" from ").append(getMailAddresses(mimeMessage.getFrom()));
			title.append(" to ").append(getMailAddresses(mimeMessage.getRecipients(Message.RecipientType.TO)));;
			titleText.setText(title.toString());
		} catch (final MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getMailAddresses(final Address[] addresses) {
		final StringBuilder sb = new StringBuilder();
		if (addresses == null || addresses.length == 0) {
			sb.append("");
		} else {
			boolean first = true;
			for(final Address address: addresses) {
				if (!first) {
					sb.append(", ");
				}
				first = false;

				if (address.getType().equalsIgnoreCase("rfc822")) {
					final InternetAddress mailAddress = (InternetAddress) address;
					if (mailAddress.getPersonal() != null) {
						sb.append(mailAddress.getPersonal());
					} else {
						sb.append(mailAddress.getAddress());
					}
				} else {
					sb.append("");
				}
			}
		}

		return sb.toString();
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
}
