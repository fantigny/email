package net.anfoya.mail.browser.javafx;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import net.anfoya.javafx.scene.control.Notification.Notifier;

public class NotificationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

	private final Stage stage;

	private final Image originalIcon;

	public NotificationService(final Stage stage) {
		this.stage = stage;

		originalIcon = new Image(this.getClass().getResource("/net/anfoya/mail/img/Mail64.png").toExternalForm());
	}

	public void setIconBadge(final String text) {
		if (text.isEmpty()) {
			resetIconBadge();
		} else {
			if (System.getProperty("os.name").contains("OS X")) {
				com.apple.eawt.Application.getApplication().setDockIconBadge(text);
			} else {
				final Canvas canvas = new Canvas(originalIcon.getWidth(), originalIcon.getHeight());
				final GraphicsContext gc = canvas.getGraphicsContext2D();
				gc.drawImage(originalIcon, 0, 0);
				gc.setFill(Color.RED);
				gc.fillOval(96, 0, 160, 160);
				gc.setFill(Color.WHITE);
				gc.setFont(Font.font(null, FontWeight.BOLD, 112));
				gc.setTextAlign(TextAlignment.CENTER);
				gc.setTextBaseline(VPos.CENTER);
				gc.setLineWidth(20);
				gc.fillText(text, 176, 71, 100);

				onFxThread(() -> {
					try {
						@SuppressWarnings("unused") final Scene scene = new Scene(new StackPane(canvas));
						final WritableImage image = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
						final SnapshotParameters parameters = new SnapshotParameters();
						parameters.setFill(Color.TRANSPARENT);
						canvas.snapshot(parameters, image);

						final BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
						ImageIO.write(bImage, "png", new File("c:/Users/Fred/Desktop/image.png"));
						stage.getIcons().setAll(originalIcon);
						stage.getIcons().setAll(new Image("file:c:/Users/Fred/Desktop/image.png", false));
//						stage.getIcons().setAll(image);
					} catch (final Exception e) {
						LOGGER.error("showing badge {}", text, e);
					}
				});
			}
		}
	}

	public void resetIconBadge() {
		if (System.getProperty("os.name").contains("OS X")) {
			com.apple.eawt.Application.getApplication().setDockIconBadge(null);
		} else {
			onFxThread(() -> stage.getIcons().setAll(originalIcon));
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
