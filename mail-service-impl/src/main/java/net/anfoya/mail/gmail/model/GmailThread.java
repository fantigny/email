package net.anfoya.mail.gmail.model;

import java.util.HashSet;
import java.util.Set;

import net.anfoya.mail.model.MailThread;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

public class GmailThread extends MailThread {

	private static String findSubject(final com.google.api.services.gmail.model.Thread thread) {
		for(final MessagePartHeader h:thread.getMessages().get(0).getPayload().getHeaders()) {
			if ("Subject".equals(h.getName())) {
				return h.getValue();
			}
		}

		return "n/d";
	}

	private static Set<String> findMessageIds(final com.google.api.services.gmail.model.Thread thread) {
		final Set<String> messageIds = new HashSet<String>();
		for(final Message m: thread.getMessages()) {
			messageIds.add(m.getId());
		}

		return messageIds;
	}

	public GmailThread(final com.google.api.services.gmail.model.Thread thread) {
		super(thread.getId(), findSubject(thread), findMessageIds(thread));
	}
}
