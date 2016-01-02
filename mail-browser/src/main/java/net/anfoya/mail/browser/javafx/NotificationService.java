package net.anfoya.mail.browser.javafx;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.anfoya.javafx.scene.control.Notification.Notifier;

public class NotificationService {
	private final Stage stage;

	private Image originalIcon = null;

	public NotificationService(Stage stage) {
		this.stage = stage;
		originalIcon = stage.getIcons().get(0);
	}

	public void setIconBadge(String text) {
		if (text.isEmpty()) {
			resetIconBadge();
		} else {
			if (System.getProperty("os.name").contains("OS X")) {
				com.apple.eawt.Application.getApplication().setDockIconBadge(text);
			} else {
				final Canvas canvas = new Canvas(originalIcon.getWidth(), originalIcon.getHeight());
				final GraphicsContext gc = canvas.getGraphicsContext2D();
				gc.drawImage(originalIcon, originalIcon.getWidth(), originalIcon.getHeight());
				gc.fillOval(225, 31, 250, 56);
				onApplicationThread(() -> stage.getIcons().setAll(canvas.snapshot(null, null)));
			}
		}
	}

	public void resetIconBadge() {
		if (System.getProperty("os.name").contains("OS X")) {
			com.apple.eawt.Application.getApplication().setDockIconBadge(null);
		} else {
			if (originalIcon != null) {
				onApplicationThread(() -> stage.getIcons().setAll(originalIcon));
			}
		}
	}

	public void notifySuccess(String subject, String value, Runnable callback) {
		onApplicationThread(() -> Notifier.INSTANCE.notifySuccess(subject, value, callback));
	}

	private void onApplicationThread(Runnable runnable) {
		if (Platform.isFxApplicationThread()) {
			runnable.run();
		} else {
			Platform.runLater(runnable);
		}
	}
}
