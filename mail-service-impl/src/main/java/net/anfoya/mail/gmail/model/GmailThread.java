package net.anfoya.mail.gmail.model;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import net.anfoya.mail.model.SimpleThread;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;

@SuppressWarnings("serial")
public class GmailThread extends SimpleThread {

	public GmailThread(final Thread thread, final String unreadTagId) {
		this(
			thread.getId()
			, findSubject(thread)
			, getMessageIds(thread)
			, getLabelIds(thread)
			, getLabelIds(thread).contains(unreadTagId)
			, findFrom(thread)
			, findReceived(thread));
	}

	public GmailThread(final String id, final String subject
			, final Set<String> messageIds, final Set<String> tagIds
			, final boolean unread, final String sender, final Date received) {
		super(id, subject, messageIds, tagIds, unread, sender, received);
	}

	private static String findSubject(final Thread thread) {
		return findHeader(thread, "Subject");
	}

	private static String findFrom(final Thread thread) {
		String from = findHeader(thread, "From");
		if (from.contains(" <")) {
			from = from.substring(0, from.indexOf(" <"));
		}
		return from;
	}

	private static Date findReceived(final Thread thread) {
		final String received = findHeader(thread, "Received");
		return new Date();
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

	private static String findHeader(final Thread thread, final String key) {
		for(final MessagePartHeader h:thread.getMessages().get(0).getPayload().getHeaders()) {
			if (key.equals(h.getName())) {
				final String subject = h.getValue();
				return subject.isEmpty()? EMPTY: subject;
			}
		}

		return EMPTY;
	}
}
