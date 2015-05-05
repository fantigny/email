package net.anfoya.mail.service;

import java.util.Set;

import javax.security.auth.login.LoginException;

import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Thread;
import net.anfoya.tag.model.Section;
import net.anfoya.tag.model.Tag;
import net.anfoya.tag.service.TagService;

public interface MailService<S extends Section, T extends Tag> extends TagService<S, T> {

	public void login(String id, String pwd) throws LoginException;
	public void logout();
	public Set<? extends Thread> getThreads(Set<T> availableTags, Set<T> includes, Set<T> excludes) throws MailServiceException;
	public Message getMessage(String id);
}
