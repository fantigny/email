package net.anfoya.mail.browser.javafx;

import java.util.Set;

import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadPane<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread> extends BorderPane {
	private final MailService<S, T, H> mailService;

	private final TextField subjectField;
	private final Accordion messageAcc;

	public ThreadPane(final MailService<S, T, H> mailService) {
		this.mailService = mailService;

		setPadding(new Insets(5));

		subjectField = new TextField("select a thread");
		BorderPane.setMargin(subjectField, new Insets(0, 0, 5, 0));
		setTop(subjectField);

		messageAcc = new Accordion();
		setCenter(messageAcc);

	}

	public void load(final Set<H> threads) {
		messageAcc.getPanes().clear();
		switch (threads.size()) {
		case 0:
			subjectField.setText("select a thread");
			break;
		case 1:
			load(threads.iterator().next());
			break;
		default:
			subjectField.setText("multiple thread selected");
			break;
		}
	}

	private void load(final H thread) {
		if (thread == null) {
			subjectField.setText("no thread selected");
			return;
		}

		subjectField.setText(thread.getSubject());

		for(final String id: thread.getMessageIds()) {
			final MessagePane pane = new MessagePane(mailService);
			messageAcc.getPanes().add(0, pane);
			pane.load(id);
		}

		if (!messageAcc.getPanes().isEmpty()) {
			messageAcc.setExpandedPane(messageAcc.getPanes().get(0));
		}
	}
}
