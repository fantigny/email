package net.anfoya.mail.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public interface Thread extends Serializable {
	public static final String PAGE_TOKEN_ID = "no-id-page-token";

	public String getId();
	public String getLastMessageId();

	public Date getDate();
	public String getSender();
	public String getSubject();

	public Set<String> getTagIds();
	public Set<String> getMessageIds();

	public boolean isUnread();
	public boolean isFlagged();
}
