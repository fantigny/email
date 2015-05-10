package net.anfoya.mail.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public interface Thread extends Serializable {

	public String getId();
	public Set<String> getTagIds();
	public Set<String> getMessageIds();

	public String getSender();
	public String getSubject();
	public String getSnippet();
	public Date getSentDate();
	public boolean isUnread();
}
