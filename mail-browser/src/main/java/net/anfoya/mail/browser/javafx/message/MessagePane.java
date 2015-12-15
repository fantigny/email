package net.anfoya.mail.browser.javafx.message;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Set;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.web.WebViewFitContent;
import net.anfoya.mail.browser.javafx.attachment.AttachmentLoader;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.util.UrlHelper;
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

public class MessagePane<M extends Message, C extends Contact> extends VBox {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessagePane.class);

    private static final Image ATTACHMENT = new Image(ThreadPane.class.getResourceAsStream("/net/anfoya/mail/img/attachment.png"));
    private static final Image MINI_ATTACHMENT = new Image(ThreadPane.class.getResourceAsStream("/net/anfoya/mail/img/mini_attach.png"));
    private static final String DEFAULT_CSS = ThreadPool.class.getResource("/net/anfoya/mail/css/default_browser.css").toExternalForm();

	private final String messageId;
	private final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService;
	private final BooleanProperty expanded;
	private final BooleanProperty collapsible;

	private final MessageHelper helper;

	private final HBox iconBox;
	private final TextFlow recipientFlow;
	private final Text dateText;
	private final FlowPane attachmentPane;
	private final WebView snippetView;
	private final VBox titlePane;
	private final WebViewFitContent messageView;

	private M message;
	private Task<String> loadTask;

	private EventHandler<ActionEvent> updateHandler;
	private volatile boolean mouseOver;

	private Timeline showSnippetTimeline;
	private Timeline showMessageTimeline;
	private EventHandler<ActionEvent> attachmentHandler;

	private VBox arrowBox;

	public MessagePane(final String messageId, final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService) {
		getStyleClass().add("message");
		this.mailService = mailService;
		this.messageId = messageId;
		expanded = new SimpleBooleanProperty();
		collapsible = new SimpleBooleanProperty();
		helper = new MessageHelper();

		messageView = new WebViewFitContent();
		messageView.focusTraversableProperty().bind(focusTraversableProperty());
		messageView.getEngine().setUserStyleSheetLocation(DEFAULT_CSS);
		messageView.getEngine().setCreatePopupHandler(handler -> messageView.getEngine());
		messageView.getEngine().locationProperty().addListener((ov, o, n) -> handleExtLink(messageView.getWebView(), n));

		recipientFlow = new TextFlow(new Text("loading..."));
		dateText = new Text();

		iconBox = new HBox();
		iconBox.setAlignment(Pos.BASELINE_RIGHT);
		iconBox.setPadding(new Insets(3, 5, 0, 0));

		snippetView = new WebView();
		snippetView.setFocusTraversable(false);
		snippetView.prefWidthProperty().bind(widthProperty());
		snippetView.setPrefHeight(30);
		snippetView.setMinHeight(0);
		snippetView.setMaxHeight(0);
		snippetView.toBack();
		snippetView.getEngine().locationProperty().addListener((ov, o, n) -> handleExtLink(snippetView, n));

		attachmentPane = new FlowPane(Orientation.HORIZONTAL, 5, 0);
		attachmentPane.setPadding(new Insets(0, 10, 0, 10));

        arrowBox = new VBox();
        arrowBox.getStyleClass().add("arrow-button");

        final StackPane arrow = new StackPane();
        arrow.getStyleClass().add("arrow");
        arrow.rotateProperty().bind(new DoubleBinding() {
        	{
        		bind(expandedProperty());
        	}
        	@Override protected double computeValue() {
        		return -90 * (1.0 - (isExpanded()? 1: 0));
        	}
        });

		final HBox title = new HBox(arrowBox, recipientFlow, iconBox, dateText);
		title.setMinHeight(27);
		HBox.setHgrow(iconBox, Priority.ALWAYS);
		title.getStyleClass().add("header");
		title.setOnMouseClicked(event -> {
			if (collapsible.get()) {
				expanded.set(!expanded.get());
			}
		});

		titlePane = new VBox(title, attachmentPane, snippetView);
		titlePane.setAlignment(Pos.CENTER_LEFT);

		titlePane.setOnMouseEntered(e ->{
			 showSnippet(expanded.not().get());
			 if (expanded.not().get()) {
				 showAttachment(true);
			 }
			 mouseOver = true;
		});
		titlePane.setOnMouseExited(e -> {
			 showSnippet(false);
			 if (expanded.not().get()) {
				 showAttachment(false);
			 }
			 mouseOver = false;
		});

		expanded.addListener((ov, o, n) -> {
			if (collapsible.not().get()) {
				return;
			}
			showMessage(n);
			showAttachment(n);
			if (!o && n) {
				snippetView.setMaxHeight(0);
			} else {
				showSnippet(mouseOver && !n);
				showAttachment(mouseOver && !n);
			}
		});
		expanded.set(true);

		collapsible.addListener((ov, o, n) -> {
			if (n) {
				title.setCursor(Cursor.HAND);
				arrowBox.getChildren().setAll(arrow);
			} else {
				title.setCursor(Cursor.DEFAULT);
				arrowBox.getChildren().clear();
			}
		});
		collapsible.set(true);

		getChildren().addAll(titlePane, messageView);
	}

	private void handleExtLink(WebView view, String url) {
		if (url != null && !url.isEmpty()) {
			Platform.runLater(() -> {
				view.getEngine().getLoadWorker().cancel();
				load();
			});
			UrlHelper.open(url, recipient -> {
				try {
					new MailComposer<M, C>(mailService, updateHandler).newMessage(recipient);
				} catch (final MailException e) {
					LOGGER.error("create new mail to {}", recipient, e);
				}
				return null;
			});
		}
	}

	private void showSnippet(final boolean show) {
		if (showSnippetTimeline != null) {
			showSnippetTimeline.stop();
		}

		final KeyValue values = new KeyValue(snippetView.maxHeightProperty(), show? snippetView.getPrefHeight(): 0);
		final KeyFrame frame = new KeyFrame(Duration.millis(100 * (show? 1: .5)), values);
		showSnippetTimeline = new Timeline(frame);
		showSnippetTimeline.play();
	}

	private void showAttachment(final boolean show) {
		if (show && !titlePane.getChildren().contains(attachmentPane)) {
			titlePane.getChildren().add(1, attachmentPane);
		} else if (!show && titlePane.getChildren().contains(attachmentPane)) {
			titlePane.getChildren().remove(attachmentPane);
		}
	}

	private void showMessage(final boolean show) {
		if (showMessageTimeline != null) {
			showMessageTimeline.stop();
		}

		final KeyValue values = new KeyValue(messageView.maxHeightProperty(), show? messageView.getPrefHeight(): 0);
		final KeyFrame frame = new KeyFrame(Duration.millis(50 * (show? 1: .5)), values);
		showMessageTimeline = new Timeline(frame);
		showMessageTimeline.play();
	}

	public synchronized void load() {
		if (loadTask != null) {
			loadTask.cancel();
		}
		loadTask = new Task<String>() {
			@Override
			protected String call() throws MailException, MessagingException, IOException, URISyntaxException {
				message = mailService.getMessage(messageId);
			    return helper.toHtml(message.getMimeMessage());
			}
		};
		loadTask.setOnSucceeded(e -> {
			refresh();
			messageView.getEngine().loadContent(loadTask.getValue());
		});
		loadTask.setOnFailed(e -> LOGGER.error("load message {}", messageId, e.getSource().getException()));
		ThreadPool.getInstance().submitHigh(loadTask, "load message " + messageId);
	}

	public void setScrollHandler(final EventHandler<ScrollEvent> handler) {
		messageView.setScrollHandler(handler);
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
		this.collapsible.set(collapsible);
	}

	public boolean isCollapsible() {
		return collapsible.get();
	}

	public BooleanProperty collapsibleProperty() {
		return collapsible;
	}

	public void setUpdateHandler(final EventHandler<ActionEvent> handler) {
		this.updateHandler = handler;
	}

	private Node buildRecipientLabel(Address address) {
		final InternetAddress internetAddress = (InternetAddress) address;
		final Label label = new Label(MessageHelper.getName(internetAddress));
		label.setTooltip(new Tooltip(internetAddress.getAddress()));
		return label;
	}

	private void refresh() {
		final MimeMessage mimeMessage = message.getMimeMessage();
		try {
			recipientFlow.getChildren().setAll(buildRecipientLabel(mimeMessage.getFrom()[0]));
			if (mimeMessage.getRecipients(MimeMessage.RecipientType.TO) != null) {
				recipientFlow.getChildren().add(new Label(" to "));
				for(final Address a: mimeMessage.getRecipients(MimeMessage.RecipientType.TO)) {
					if (recipientFlow.getChildren().size() > 2) {
						recipientFlow.getChildren().add(new Label(", "));
					}
					recipientFlow.getChildren().add(buildRecipientLabel(a));
				}
			}
		} catch (final MessagingException e) {
			LOGGER.error("get title data", e);
		}

		String text = "";
		try {
			final String[] headers = mimeMessage.getHeader("Received", null).split(";");
			final String dateString = headers[headers.length -1].trim();
			final Date date = new MailDateFormat().parse(dateString);
			text = new DateHelper(date).format();
		} catch (final Exception ex1) {
			try {
				text = new DateHelper(mimeMessage.getSentDate()).format();
			} catch (final Exception ex2) {
				LOGGER.error("get received date", ex1);
				LOGGER.error("get sent date", ex2);
				text = "n/d";
			}
		}
		dateText.setText(text);

		snippetView.getEngine().loadContent(message.getSnippet() + "...");
		snippetView.getEngine().setUserStyleSheetLocation(DEFAULT_CSS);

		attachmentPane.getChildren().clear();
		final Set<String> attachNames = helper.getAttachmentNames();
		if (!attachNames.isEmpty()) {
			for(final String name: attachNames) {
				final HBox attachment = new HBox(3, new ImageView(ATTACHMENT), new Label(name));
				attachment.setPadding(new Insets(5));
				attachment.setCursor(Cursor.HAND);
				attachment.setOnMouseClicked(e -> {
					try {
						new AttachmentLoader<M>(mailService, message.getId()).start(name);
					} catch (final Exception ex) {
						LOGGER.error("load ", ex);
					}
				});
				attachmentPane.getChildren().add(attachment);
			}
			iconBox.getChildren().add(new ImageView(MINI_ATTACHMENT));
			attachmentHandler.handle(null);
		}
	}

	public void onContainAttachment(EventHandler<ActionEvent> handler) {
		attachmentHandler = handler;
	}

	public boolean hasAttachment() {
		return !helper.getAttachmentNames().isEmpty();
	}
}
