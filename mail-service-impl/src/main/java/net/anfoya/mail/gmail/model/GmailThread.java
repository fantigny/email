package net.anfoya.mail.gmail.model;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.mail.internet.MailDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;

import net.anfoya.mail.model.SimpleThread;

@SuppressWarnings("serial")
public class GmailThread extends SimpleThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(GmailThread.class);

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
		String received = findHeader(thread, "Received");
		try {
			received = received.substring(received.lastIndexOf(";")+1, received.length()).trim();
			return new MailDateFormat().parse(received);
		} catch (final ParseException e) {
			LOGGER.error("parse received date: {}", received, e);
			return new Date();
		}
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
		if (thread.getMessages() != null
				&& !thread.getMessages().isEmpty()) {
			final Message last = thread.getMessages().get(thread.getMessages().size() - 1);
			if (last.getPayload() != null
					&& last.getPayload().getHeaders() != null
					&& !last.getPayload().isEmpty()) {
				for(final MessagePartHeader h: last.getPayload().getHeaders()) {
					if (key.equalsIgnoreCase(h.getName()) && !h.getValue().isEmpty()) {
						headers.add(h.getValue());
					}
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
