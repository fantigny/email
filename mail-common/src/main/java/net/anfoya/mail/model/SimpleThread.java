package net.anfoya.mail.model;

import java.util.Comparator;
import java.util.Date;
import java.util.Set;

@SuppressWarnings("serial")
public abstract class SimpleThread implements Thread {
	public enum SortType {
		DATE((t1, t2) -> {
			if (t1.getDate() == null || PAGE_TOKEN_ID.equals(t2.getId())) {
				return -1;
			} else if (t2.getDate() == null || PAGE_TOKEN_ID.equals(t1.getId())) {
				return 1;
			} else {
				return t2.getDate().compareTo(t1.getDate());
			}
		}),
		SENDER((t1, t2) -> {
			if (t1.getSender() == null || PAGE_TOKEN_ID.equals(t2.getId())) {
				return -1;
			} else if (t2.getSender() == null || PAGE_TOKEN_ID.equals(t1.getId())) {
				return 1;
			} else {
				return t1.getSender().compareTo(t2.getSender());
			}
		});

		private Comparator<Thread> comparator;
		private SortType(final Comparator<Thread> comparator) {
			this.comparator = comparator;
		}

		public Comparator<Thread> getComparator() {
			return comparator;
		}
	}

	private final String id;
	private final int hash;
	private final String subject;
	private final Set<String> messageIds;
	private final Set<String> tagIds;
	private final String sender;
	private final Set<String> recipients;
	private final Date date;
	private final String lastMessageId;

	public SimpleThread(final String id
			, final String subject
			, final Set<String> messageIds
			, final String lastMessageId
			, final Set<String> tagIds
			, final String sender
			, final Set<String> recipients
			, final Date date) {
		this.id = id;
		this.hash = id.hashCode();
		this.subject = subject;
		this.messageIds = messageIds;
		this.tagIds = tagIds;
		this.sender = sender;
		this.recipients = recipients;
		this.date = date;
		this.lastMessageId = lastMessageId;
	}

	@Override
	public String toString() {
		return subject;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public Set<String> getMessageIds() {
		return messageIds;
	}

	@Override
	public String getLastMessageId() {
		return lastMessageId;
	}

	@Override
	public Set<String> getTagIds() {
		return tagIds;
	}

	@Override
	public String getSender() {
		return sender;
	}

	@Override
	public Set<String> getRecipients() {
		return recipients;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public int hashCode() {
		return hash;
	}

    @Override
	public boolean equals(final Object that) {
    	return this == that || that instanceof SimpleThread && id.equals(((SimpleThread) that).id);
    }

}
