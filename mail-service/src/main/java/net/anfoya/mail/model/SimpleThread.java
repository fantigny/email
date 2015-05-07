package net.anfoya.mail.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;

@SuppressWarnings("serial")
public class SimpleThread implements Serializable {
	protected static final String EMPTY = "[empty]";

	public enum SortOrder {
		DATE(new Comparator<SimpleThread>() {
			@Override
			public int compare(final SimpleThread s1, final SimpleThread s2) {
				return s2.getSentDate().compareTo(s1.getSentDate());
			}
		}),
		SUBJECT(new Comparator<SimpleThread>() {
			@Override
			public int compare(final SimpleThread s1, final SimpleThread s2) {
				//TODO: change to sender sort
				return s1.getSubject().compareTo(s2.getSubject());
			}
		});

		private Comparator<SimpleThread> comparator;
		private SortOrder(final Comparator<SimpleThread> comparator) {
			this.comparator = comparator;
		}

		public Comparator<SimpleThread> getComparator() {
			return comparator;
		}
	}

	private final String id;
	private final String subject;
	private final Set<String> messageIds;
	private final Set<String> tagIds;

	public SimpleThread(final String id, final String subject, final Set<String> messageIds, final Set<String> tagIds) {
		this.id = id;
		this.subject = subject;
		this.messageIds = messageIds;
		this.tagIds = tagIds;
	}

	public Date getSentDate() {
		// TODO Auto-generated method stub
		return new Date();
	}

	@Override
	public String toString() {
		return subject;
	}

	public String getId() {
		return id;
	}

	public String getSubject() {
		return subject;
	}

	public Set<String> getMessageIds() {
		return messageIds;
	}

	public Set<String> getTagIds() {
		return tagIds;
	}
}
