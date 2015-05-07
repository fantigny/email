package net.anfoya.mail.gmail.model;

import java.util.LinkedHashSet;
import java.util.Set;

import javafx.concurrent.Task;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;

public class GmailThread extends SimpleThread {

	private static String findSubject(final com.google.api.services.gmail.model.Thread thread) {
		for(final MessagePartHeader h:thread.getMessages().get(0).getPayload().getHeaders()) {
			if ("Subject".equals(h.getName())) {
				final String subject = h.getValue();
				return subject.isEmpty()? EMPTY: subject;
			}
		}

		return EMPTY;
	}

	private static Set<String> findMessageIds(final Thread thread) {
		final Set<String> messageIds = new LinkedHashSet<String>();
		for(final Message m: thread.getMessages()) {
			messageIds.add(m.getId());
		}

		return messageIds;
	}

	private LinkedHashSet<String> messageIds;
	private String subject;

	public GmailThread(final Thread thread) {
		super(thread.getId(), findSubject(thread), findMessageIds(thread));
	}

	public GmailThread(final String id, final MailService<GmailSection, GmailTag, GmailThread> mailService) {
		super(id, null, null);
		final Task<GmailThread> task = new Task<GmailThread>() {
			@Override
			protected GmailThread call() throws Exception {
				return mailService.getThread(id);
			}
		};
		task.setOnSucceeded(event -> {
			try {
				final GmailThread thread = task.get();
				messageIds = new LinkedHashSet<String>(thread.getMessageIds());
				subject = thread.getSubject();
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	@Override
	public Set<String> getMessageIds() {
		return messageIds;
	}

	@Override
	public String getSubject() {
		return subject;
	}
}
