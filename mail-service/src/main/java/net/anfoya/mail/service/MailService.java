package net.anfoya.mail.service;

import java.util.List;

import javax.security.auth.login.LoginException;

import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Thread;
import net.anfoya.tag.model.Tag;
import net.anfoya.tag.service.TagService;

public interface MailService extends TagService {

	public void login(String id, String pwd) throws LoginException;
	public void logout();
	public List<Thread> getThreads(List<Tag> tags) throws MailServiceException;
	public List<String> getMessageIds(String threadId);
	public Message getMessage(String id);
}
