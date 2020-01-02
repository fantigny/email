package net.anfoya.mail.yahoo.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;

import net.anfoya.mail.model.SimpleThread;

@SuppressWarnings("serial")
public class YahooThread extends SimpleThread {

	public YahooThread(Message m) throws MessagingException {
		this(""
				, m.getSubject()
				, Collections.singleton("1")
				, "1"
				, Collections.emptySet()
				, m.getFrom()[0].toString()
				, findRecipients(m)
				, m.getReceivedDate());
	}

	private static Set<String> findRecipients(Message m) throws MessagingException {
		Set<String> recipients = new HashSet<>();
		Arrays.stream(m.getRecipients(RecipientType.TO)).forEach(r -> recipients.add(r.toString()));

		return recipients;
	}

	public YahooThread(final String id, final String subject, final Set<String> messageIds, final String lastMessageId, final Set<String> tagIds,
			final String sender, final Set<String> recipients, final Date received) {
		super(id, subject, messageIds, lastMessageId, tagIds, sender, recipients, received);
	}

	@Override
	public boolean isUnread() {
		return false;
	}

	@Override
	public boolean isFlagged() {
		return false;
	}

	@Override
	public boolean isSpam() {
		return false;
	}
}
