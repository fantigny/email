package net.anfoya.mail.browser.javafx.message;

import javafx.scene.Scene;
import javafx.stage.Stage;
import net.anfoya.mail.browser.javafx.css.StyleHelper;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;

public class MailReader extends Stage {
	public MailReader(ThreadPane<?, ?, ?, ?, ?> pane) {
		setScene(new Scene(pane, 800, 600));
		setTitle("FisherMail - reader");
		StyleHelper.addCommonCss(getScene());
	}
}
