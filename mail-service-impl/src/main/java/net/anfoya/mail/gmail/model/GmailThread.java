package net.anfoya.mail.gmail.model;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.mail.internet.MailDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;

import net.anfoya.mail.model.SimpleThread;

@SuppressWarnings("serial")
public class GmailThread extends SimpleThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(GmailThread.class);

	private static String findSubject(final Thread thread) {
		return findHeader(thread, "Subject");
	}

	private static Set<String> findRecipients(Thread thread) {
		return findHeaders(thread, "To").parallelStream().map(s -> cleanAddress(s)).collect(Collectors.toSet());
	}

	private static String findSender(final Thread thread) {
		if (thread.getMessages() != null
				&& thread.getMessages().size() == 1
				&& thread.getMessages().get(0).getLabelIds().contains(GmailTag.SENT.getId())) {
			return findRecipients(thread)
					.stream()
					.reduce("", (s, r) -> s.length() == 0 ? r : ", " + r);
		}
		return cleanAddress(findHeader(thread, "From"));
	}

	private static String cleanAddress(String address) {
		final int index = address.indexOf(" <");
		if (index != -1) {
			address = address.substring(0, index);
		}
		return address.replaceAll("\"|'", "");
	}

	private static Date findReceivedDate(final Thread thread) {
		Date date;
		String received = findHeader(thread, "Received");
		if (received.isEmpty()) {
			received = findHeader(thread, "Date");
		}
		try {
			received = received.substring(received.lastIndexOf(";") + 1, received.length()).trim();
			date = new MailDateFormat().parse(received);
		} catch (final ParseException e) {
			LOGGER.error("parse received date: {}", received, e);
			date = null;
		}
		if (date == null) {
			date = new Date();
		}
		return date;
	}

	private static Set<String> getMessageIds(final Thread thread) {
		final Set<String> messageIds = new LinkedHashSet<>();
		if (thread.getMessages() != null) {
			for (final Message m : thread.getMessages()) {
				messageIds.add(m.getId());
			}
		}

		return messageIds;
	}

	private static Message getLastMessage(Thread thread) {
		final List<Message> messages = thread.getMessages();
		if (messages == null || messages.isEmpty()) {
			return null;
		}

		Message last = null;
		for(int i=messages.size()-1; i>=0; i--) {
			last = messages.get(i);
			if (!last.getLabelIds().contains(GmailTag.SENT.getId())) {
				break;
			}
		}
		return last;
	}

	private static Set<String> getTagIds(final Thread thread) {
		if (thread.getMessages() == null) {
			return Collections.emptySet();
		}

		return thread
				.getMessages()
				.parallelStream()
				.flatMap(m -> m.getLabelIds().parallelStream())
				.collect(Collectors.toSet());
	}

	private static String findHeader(final Thread thread, final String key) {
		final Set<String> headers = findHeaders(thread, key);
		if (headers.isEmpty()) {
			headers.add("");
		}
		return headers.iterator().next();
	}

	private static Set<String> findHeaders(final Thread thread, final String key) {
		final Set<String> headers = new LinkedHashSet<>();
		if (thread.getMessages() != null && !thread.getMessages().isEmpty()) {
			final Message last = getLastMessage(thread);
			if (last.getPayload() != null && last.getPayload().getHeaders() != null) {
				last.getPayload().getHeaders().forEach(h -> {
					if (!h.getValue().isEmpty() && h.getName().equalsIgnoreCase(key)) {
						headers.add(h.getValue());
					}
				});
			}
		}

		return headers;
	}

	public GmailThread(final Thread thread) {
		this(thread.getId()
				, findSubject(thread)
				, getMessageIds(thread)
				, getLastMessage(thread).getId()
				, getTagIds(thread)
				, findSender(thread)
				, findRecipients(thread)
				, findReceivedDate(thread));
	}

	public GmailThread(final String id, final String subject, final Set<String> messageIds, final String lastMessageId, final Set<String> tagIds,
			final String sender, final Set<String> recipients, final Date received) {
		super(id, subject, messageIds, lastMessageId, tagIds, sender, recipients, received);
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
