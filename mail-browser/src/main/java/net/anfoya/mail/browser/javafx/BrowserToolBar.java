package net.anfoya.mail.browser.javafx;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;

public class BrowserToolBar extends ToolBar {
	private static final Separator SEPARATOR = new Separator();

    private static final String IMG_PATH = "/net/anfoya/mail/img/";
    private static final String SETTINGS_PNG = ThreadPane.class.getResource(IMG_PATH + "settings.png").toExternalForm();
    private static final String SIGNOUT_PNG = ThreadPane.class.getResource(IMG_PATH + "signout.png").toExternalForm();

	private Runnable composeCallback;
	private Runnable showSettingsCallback;
	private Runnable signoutCallback;

	private final Button composeButton;
	private final Button settingsButton;
	private final Button signoutButton;


	public BrowserToolBar() {
		setMinHeight(27);
		setMaxHeight(27);
		setMinWidth(ToolBar.USE_PREF_SIZE);

		composeButton = new Button();
		composeButton.getStyleClass().add("flat-button");
		composeButton.setFocusTraversable(false);
		composeButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/new.png"))));
		composeButton.setTooltip(new Tooltip("new"));
		composeButton.setOnAction(e -> composeCallback.run());

		settingsButton = new Button();
		settingsButton.getStyleClass().add("flat-button");
		settingsButton.setFocusTraversable(false);
		settingsButton.setGraphic(new ImageView(new Image(SETTINGS_PNG)));
		settingsButton.setTooltip(new Tooltip("preferences"));
		settingsButton.setOnAction(e -> showSettingsCallback.run());

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
		signoutButton.setOnAction(e -> signoutCallback.run());
	}


	public void setVisibles(boolean newMessage, boolean settings, boolean signout) {
		setVisible(composeButton, newMessage);
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

	public void setOnSignout(Runnable callback) {
		signoutCallback = callback;
	}

	public void setOnShowSettings(Runnable callback) {
		showSettingsCallback = callback;
	}

	public void setOnCompose(Runnable callback) {
		composeCallback = callback;
	}
}
