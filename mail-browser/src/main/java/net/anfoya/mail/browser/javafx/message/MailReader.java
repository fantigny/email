package net.anfoya.mail.browser.javafx.message;

import javafx.scene.Scene;
import javafx.stage.Stage;
import net.anfoya.mail.browser.javafx.css.CssHelper;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.client.App;

public class MailReader extends Stage {
	public MailReader(ThreadPane<?, ?, ?, ?, ?> pane) {
		setScene(new Scene(pane, 800, 600));
		setTitle(App.getName() + " - reader");
		CssHelper.addCommonCss(getScene());
	}
}
