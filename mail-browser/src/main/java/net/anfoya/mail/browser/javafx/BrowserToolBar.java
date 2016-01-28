package net.anfoya.mail.browser.javafx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.settings.SettingsDialog;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;

public class BrowserToolBar<S extends Section, T extends Tag, M extends Message, C extends Contact> extends ToolBar {
	private static final Logger LOGGER = LoggerFactory.getLogger(BrowserToolBar.class);
	private static final Separator SEPARATOR = new Separator();

    private static final String IMG_PATH = "/net/anfoya/mail/img/";
    private static final String SETTINGS_PNG = ThreadPane.class.getResource(IMG_PATH + "settings.png").toExternalForm();
    private static final String SIGNOUT_PNG = ThreadPane.class.getResource(IMG_PATH + "signout.png").toExternalForm();

	private EventHandler<ActionEvent> signoutHandler;
	private SettingsDialog<S, T> settingsDialog;

	private final MailService<S, T, ?, M, C> mailService;

	private final UndoService undoService;
	private Runnable messageUpdateCallback;

	private final Button newButton;

	private final Button settingsButton;

	private final Button signoutButton;


	public BrowserToolBar(MailService<S, T, ?, M, C> mailService, UndoService undoService, Settings settings) {
		this.mailService = mailService;
		this.undoService = undoService;

		setMinHeight(27);
		setMaxHeight(27);
		setMinWidth(ToolBar.USE_PREF_SIZE);

		newButton = new Button();
		newButton.getStyleClass().add("flat-button");
		newButton.setFocusTraversable(false);
		newButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/new.png"))));
		newButton.setTooltip(new Tooltip("new"));
		newButton.setOnAction(event -> {
			try {
				final MailComposer<M, C> composer = new MailComposer<M, C>(mailService, settings);
				composer.setOnMessageUpdate(messageUpdateCallback);
				composer.newMessage("");
			} catch (final Exception e) {
				LOGGER.error("load new message composer", e);
			}
		});

		settingsButton = new Button();
		settingsButton.getStyleClass().add("flat-button");
		settingsButton.setFocusTraversable(false);
		settingsButton.setGraphic(new ImageView(new Image(SETTINGS_PNG)));
		settingsButton.setTooltip(new Tooltip("settings"));
		settingsButton.setOnAction(e -> showSettings(settings));

		final Node graphics = settingsButton.getGraphic();

		final RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), graphics);
		rotateTransition.setByAngle(360);
		rotateTransition.setCycleCount(Timeline.INDEFINITE);
		rotateTransition.setInterpolator(Interpolator.EASE_IN);

		final RotateTransition stopRotateTransition = new RotateTransition(Duration.INDEFINITE, graphics);
		rotateTransition.setInterpolator(Interpolator.EASE_OUT);

		ThreadPool.getDefault().addOnChange(PoolPriority.MAX, map -> {
			if (map.isEmpty()) {
				rotateTransition.stop();
				stopRotateTransition.setByAngle(360d - graphics.getRotate() % 360d);
				stopRotateTransition.setDuration(Duration.seconds(.5 * stopRotateTransition.getByAngle() / 360d));
				stopRotateTransition.play();
			} else {
				stopRotateTransition.stop();
				rotateTransition.play();
			}
		});

		signoutButton = new Button();
		signoutButton.getStyleClass().add("flat-button");
		signoutButton.setFocusTraversable(false);
		signoutButton.setGraphic(new ImageView(new Image(SIGNOUT_PNG)));
		signoutButton.setTooltip(new Tooltip("sign out"));
		signoutButton.setOnAction(e -> signoutHandler.handle(null));
	}


	public void setVisibles(boolean newMessage, boolean settings, boolean signout) {
		setVisible(newButton, newMessage);
		SEPARATOR.setVisible(newMessage && settings || signout);
		setVisible(settingsButton, settings);
		setVisible(signoutButton, signout);
	}

	private void setVisible(Button button, boolean visible) {
		if (visible && !getItems().contains(button)) {
			getItems().add(button);
		} else if (!visible && getItems().contains(button)) {
			getItems().remove(button);
		}
	}

	public void setOnSignout(final EventHandler<ActionEvent> handler) {
		signoutHandler = handler;
	}

	private void showSettings(final Settings settings) {
		if (settingsDialog == null
				|| !settingsDialog.isShowing()) {
			settingsDialog = new SettingsDialog<S, T>(mailService, undoService, settings);
			settingsDialog.show();
		}
		settingsDialog.toFront();
		settingsDialog.requestFocus();
	}

	public void setOnMessageUpdate(Runnable callback) {
		messageUpdateCallback = callback;
	}
}
