package net.anfoya.mail.service;

import java.util.Set;

import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;

public interface MailService {

	public void login(String id, String pwd) throws MailServiceException;
	public void logout();
	public Set<Thread> getThreads(Set<Tag> tags) throws MailServiceException;
	public Set<Tag> getTags() throws MailServiceException;
}
