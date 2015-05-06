package net.anfoya.mail.browser.javafx;

import javafx.scene.control.Accordion;
import javafx.scene.layout.BorderPane;
import net.anfoya.mail.model.MailThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.TagSection;
import net.anfoya.tag.model.ThreadTag;

public class ThreadPane extends BorderPane {
	private final MailService<? extends TagSection, ? extends ThreadTag, ? extends MailThread> mailService;

	private final Accordion messageAcc;

	public ThreadPane(final MailService<? extends TagSection, ? extends ThreadTag, ? extends MailThread> mailService) {
		this.mailService = mailService;

		messageAcc = new Accordion();
		setCenter(messageAcc);
	}

	public void load(final MailThread thread) {
		messageAcc.getPanes().clear();
		if (thread == null) {
			return;
		}

		for(final String id: thread.getMessageIds()) {
			final MessagePane pane = new MessagePane(mailService);
			messageAcc.getPanes().add(pane);
			pane.load(id);
		}

		if (!messageAcc.getPanes().isEmpty()) {
			messageAcc.setExpandedPane(messageAcc.getPanes().get(0));
		}
	}
}
