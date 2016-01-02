package net.anfoya.mail.browser.javafx;

import javafx.application.Platform;
import javafx.stage.Stage;
import net.anfoya.javafx.scene.control.Notification.Notifier;

public class NotificationService {
	private final Stage stage;

	private boolean showZeroCount;

	public NotificationService(Stage stage) {
		this.stage = stage;

		showZeroCount = false;
	}

	public void setShowZeroCount(boolean show) {
		showZeroCount = show;
	}

	public void setIconCount(int count) {
		if (count == 0 && !showZeroCount) {
			resetIcon();
			return;
		}

		if (!System.getProperty("os.name").contains("OS X")) {
			com.apple.eawt.Application.getApplication().setDockIconBadge("" + count);
		} else {
//TODO			stage.getIcons().setAll(icon);
		}
	}

	public void resetIcon() {
		if (!System.getProperty("os.name").contains("OS X")) {
			com.apple.eawt.Application.getApplication().setDockIconBadge(null);
		} else {
//			final Image icon = new Image("http://icons.iconarchive.com/icons/custom-icon-design/flatastic-8/256/Refresh-icon.png");
			//TODO			stage.getIcons().setAll(icon);
		}
	}

	public void notifySuccess(String subject, String value, Runnable callback) {
		final Runnable notify = () -> Notifier.INSTANCE.notifySuccess(subject, value, callback);
		if (Platform.isFxApplicationThread()) {
			notify.run();
		} else {
			Platform.runLater(notify);
		}
	}
}
