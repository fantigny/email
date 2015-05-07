package net.anfoya.mail.gmail.model;

import java.util.LinkedHashSet;
import java.util.Set;

import net.anfoya.mail.model.SimpleThread;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;

@SuppressWarnings("serial")
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

	private static Set<String> getMessageIds(final Thread thread) {
		final Set<String> messageIds = new LinkedHashSet<String>();
		for(final Message m: thread.getMessages()) {
			messageIds.add(m.getId());
		}

		return messageIds;
	}

	private static Set<String> getLabelIds(final Thread thread) {
		final Set<String> labelIds = new LinkedHashSet<String>();
		for(final Message m: thread.getMessages()) {
			labelIds.addAll(m.getLabelIds());
		}

		return labelIds;
	}

	public GmailThread(final Thread thread) {
		super(thread.getId(), findSubject(thread), getMessageIds(thread), getLabelIds(thread));
	}
}
