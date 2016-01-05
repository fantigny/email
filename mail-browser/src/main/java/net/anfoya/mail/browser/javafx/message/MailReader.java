package net.anfoya.mail.browser.javafx.message;

import java.util.Collections;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.anfoya.java.undo.UndoService;
import net.anfoya.mail.browser.javafx.css.StyleHelper;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

public class MailReader<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends Stage {
	public MailReader(final MailService<S, T, H, M, C> mailService
			, final UndoService undoService
			, final Settings settings
			, final H thread
			, final EventHandler<ActionEvent> updateHandler) {
		final ThreadPane<S, T, H, M, C> pane = new ThreadPane<>(mailService, undoService, settings);
		pane.setOnUpdateThread(updateHandler);
		pane.refresh(Collections.singleton(thread), true);

		setScene(new Scene(pane, 800, 600));
		StyleHelper.addCommonCss(getScene());
	}
}
