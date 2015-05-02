package net.anfoya.mail.service;

import java.util.List;

import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Thread;
import net.anfoya.tag.model.Tag;

public interface MailService {

	public void login(String id, String pwd) throws MailServiceException;
	public void logout();
	public List<Thread> getThreads(List<Tag> tags) throws MailServiceException;
	public List<Tag> getTags() throws MailServiceException;
	List<String> getMessageIds(String threadId);
	public Message getMessage(String id);
}
