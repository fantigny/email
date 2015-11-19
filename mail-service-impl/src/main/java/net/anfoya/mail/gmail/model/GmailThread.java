package net.anfoya.mail.gmail.model;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;

import net.anfoya.mail.model.SimpleThread;

@SuppressWarnings("serial")
public class GmailThread extends SimpleThread {

	public GmailThread(final Thread thread) {
		this(
			thread.getId()
			, findSubject(thread)
			, getMessageIds(thread)
			, getTagIds(thread)
			, findSender(thread)
			, findReceivedDate(thread));
	}

	public GmailThread(final String id, final String subject
			, final Set<String> messageIds, final Set<String> tagIds
			, final String sender, final Date received) {
		super(id, subject, messageIds, tagIds, sender, received);
	}

	private static String findSubject(final Thread thread) {
		return findHeader(thread, "Subject");
	}

	private static String findSender(final Thread thread) {
		String from = findHeader(thread, "From");
		if (from.contains(" <")) {
			from = from.substring(0, from.indexOf(" <"));
		}
		return from.replaceAll("\"", "");
	}

	private static Date findReceivedDate(final Thread thread) {
		if (thread.getMessages() != null) {
			Date d = null;
			for(final Message m: thread.getMessages()) {
				if (m.getInternalDate() != null) {
					d = new Date(m.getInternalDate());
				}
			}
			if (d != null) {
				return d;
			}
		}

		return new Date();
	}

	private static Set<String> getMessageIds(final Thread thread) {
		final Set<String> messageIds = new LinkedHashSet<String>();
		if (thread.getMessages() != null) {
			for(final Message m: thread.getMessages()) {
				messageIds.add(m.getId());
			}
		}

		return messageIds;
	}

	private static Set<String> getTagIds(final Thread thread) {
		final Set<String> tagIds = new LinkedHashSet<String>();
		if (thread.getMessages() != null) {
			for(final Message m: thread.getMessages()) {
				tagIds.addAll(m.getLabelIds());
			}
		}

		return tagIds;
	}

	private static String findHeader(final Thread thread, final String key) {
		final Set<String> headers = findHeaders(thread, key);
		if (headers.isEmpty()) {
			headers.add("");
		}
		return headers.iterator().next();
	}

	private static Set<String> findHeaders(final Thread thread, final String key) {
		final Set<String> headers = new LinkedHashSet<String>();
		if (thread.getMessages() != null && !thread.getMessages().isEmpty()) {
			final Message message = thread.getMessages().get(thread.getMessages().size() - 1);
			for(final MessagePartHeader h: message.getPayload().getHeaders()) {
				if (key.equalsIgnoreCase(h.getName()) && !h.getValue().isEmpty()) {
					headers.add(h.getValue());
				}
			}
		}

		return headers;
	}

	@Override
	public boolean isUnread() {
		return getTagIds().contains(GmailTag.UNREAD.getId());
	}

	@Override
	public boolean isFlagged() {
		return getTagIds().contains(GmailTag.STARRED.getId());
	}
}
