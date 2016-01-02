package net.anfoya.mail.browser.javafx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Notification.Notifier;

public class NotificationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

	private final Stage stage;

	private final Image applicationIcon;
	private final SnapshotParameters snapshotParameters;

	private Task<Canvas> badgeTask;

	public NotificationService(final Stage stage) {
		this.stage = stage;

		applicationIcon = new Image(this.getClass().getResource("/net/anfoya/mail/img/Mail64.png").toExternalForm());

		snapshotParameters = new SnapshotParameters();
		snapshotParameters.setFill(Color.TRANSPARENT);

		badgeTask = null;
	}

	public synchronized void setIconBadge(final String text) {
		if (text.isEmpty()) {
			resetIconBadge();
		} else {
			if (System.getProperty("os.name").contains("OS X")) {
				com.apple.eawt.Application.getApplication().setDockIconBadge(text);
			} else {
				if (badgeTask != null) {
					badgeTask.cancel();
				}
				badgeTask = new Task<Canvas>() {
					@Override
					protected Canvas call() throws Exception {
						final Canvas canvas = new Canvas(applicationIcon.getWidth(), applicationIcon.getHeight());
						final GraphicsContext gc = canvas.getGraphicsContext2D();
						gc.drawImage(applicationIcon, 0, 0);
						gc.setFill(Color.RED);
						gc.fillOval(28, 0, 36, 36);
						gc.setFill(Color.WHITE);
						gc.setFont(Font.font("arial", FontWeight.BOLD, 24));
						gc.setTextAlign(TextAlignment.CENTER);
						gc.setTextBaseline(VPos.CENTER);
						gc.setLineWidth(20);
						gc.fillText(text, 45, 18, 32);
						return canvas;
					}
				};
				badgeTask.setOnFailed(e -> LOGGER.error("create icon badge {}", text, e.getSource().getException()));
				badgeTask.setOnSucceeded(e -> stage.getIcons().setAll(SwingFXUtils.toFXImage(SwingFXUtils.fromFXImage(
								badgeTask.getValue().snapshot(snapshotParameters, null), null), null)));
				ThreadPool.getInstance().submitLow(badgeTask, "set icon badge");
			}
		}
	}

	public void resetIconBadge() {
		if (System.getProperty("os.name").contains("OS X")) {
			com.apple.eawt.Application.getApplication().setDockIconBadge(null);
		} else {
			onFxThread(() -> stage.getIcons().setAll(applicationIcon));
		}
	}

	public void notifySuccess(final String subject, final String value, final Runnable callback) {
		onFxThread(() -> Notifier.INSTANCE.notifySuccess(subject, value, callback));
	}

	private void onFxThread(final Runnable runnable) {
		if (Platform.isFxApplicationThread()) {
			runnable.run();
		} else {
			Platform.runLater(runnable);
		}
	}
}
