package net.anfoya.mail.gmail.model;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.mail.internet.MailDateFormat;

import net.anfoya.mail.model.SimpleThread;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;

@SuppressWarnings("serial")
public class GmailThread extends SimpleThread {

	public GmailThread(final Thread thread) {
		this(
			thread.getId()
			, findSubject(thread)
			, getMessageIds(thread)
			, getLabelIds(thread)
			, findFrom(thread)
			, findDate(thread));
	}

	public GmailThread(final String id, final String subject
			, final Set<String> messageIds, final Set<String> tagIds
			, final String sender, final Date received) {
		super(id, subject, messageIds, tagIds, sender, received);
	}

	private static String findSubject(final Thread thread) {
		return findHeader(thread, "Subject");
	}

	private static String findFrom(final Thread thread) {
		String from = findHeader(thread, "From");
		if (from.contains(" <")) {
			from = from.substring(0, from.indexOf(" <"));
		}
		return from.replaceAll("\"", "");
	}

	private static Date findDate(final Thread thread) {
		try {
			final String s = findHeader(thread, "Date");
			return new MailDateFormat().parse(s);
		} catch (final Exception e) {
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

	private static Set<String> getLabelIds(final Thread thread) {
		final Set<String> labelIds = new LinkedHashSet<String>();
		if (thread.getMessages() != null) {
			for(final Message m: thread.getMessages()) {
				labelIds.addAll(m.getLabelIds());
			}
		}

		return labelIds;
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
			for(final MessagePartHeader h:thread.getMessages().get(0).getPayload().getHeaders()) {
				if (key.equals(h.getName())) {
					headers.add(h.getValue());
				}
			}
		}

		return headers;
	}
}
