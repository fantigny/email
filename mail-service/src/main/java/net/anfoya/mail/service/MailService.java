package net.anfoya.mail.service;

import java.util.List;

import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;

public interface MailService {

	public void login(String id, String pwd) throws MailServiceException;
	public void logout();
	public List<Thread> getThreads(List<Tag> tags) throws MailServiceException;
	public List<Tag> getTags() throws MailServiceException;
}
