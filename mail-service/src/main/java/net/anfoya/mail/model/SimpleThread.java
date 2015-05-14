package net.anfoya.mail.model;

import java.util.Comparator;
import java.util.Date;
import java.util.Set;

import net.anfoya.mail.service.Thread;

@SuppressWarnings("serial")
public abstract class SimpleThread implements Thread {
	protected static final String EMPTY = "[empty]";

	public enum SortOrder {
		DATE((s1, s2) -> s2.getDate().compareTo(s1.getDate())),
		SUBJECT((s1, s2) -> s1.getSubject().compareTo(s2.getSubject()));

		private Comparator<SimpleThread> comparator;
		private SortOrder(final Comparator<SimpleThread> comparator) {
			this.comparator = comparator;
		}

		public Comparator<SimpleThread> getComparator() {
			return comparator;
		}
	}

	private final String id;
	private final int hash;
	private final String subject;
	private final Set<String> messageIds;
	private final Set<String> tagIds;
	private final boolean unread;
	private final String sender;
	private final Date date;

	public SimpleThread(final String id
			, final String subject
			, final Set<String> messageIds
			, final Set<String> tagIds
			, final boolean unread
			, final String sender
			, final Date date) {
		this.id = id;
		this.hash = id.hashCode();
		this.subject = subject;
		this.messageIds = messageIds;
		this.tagIds = tagIds;
		this.unread = unread;
		this.sender = sender;
		this.date = date;
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
	public Set<String> getTagIds() {
		return tagIds;
	}

	@Override
	public boolean isUnread() {
		return unread;
	}

	@Override
	public String getSender() {
		return sender;
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
	public boolean equals(final Object other) {
        if (other == null) {
			return false;
		}
        if (!(other instanceof SimpleThread)) {
			return false;
		}
        return ((SimpleThread) other).id.equals(id);
    }
}
